package creditcards.service

import cats.MonadThrow
import cats.data.EitherT
import cats.implicits._
import creditcards.client.model.{SinglarCSCard, SinglarScoredCard}
import creditcards.client.{CSCardsClient, CardsClientError, ScoredCardsClient}
import creditcards.service.model.CardProvider.{CSCards, ScoredCards}
import creditcards.service.model._
import org.typelevel.log4cats.Logger

import scala.concurrent.TimeoutException

trait CreditCardsService[F[_]] {

  def cardsForUser(
      username: Username,
      creditScore: Int,
      salary: Salary
  ): F[List[CardDetails]]

}

class CreditCardsServiceImpl[F[_]: MonadThrow: Logger](
    csCardsClient: CSCardsClient[F],
    scoredCardsClient: ScoredCardsClient[F]
) extends CreditCardsService[F] {

  /** @param cards
    *   the list of cards from CS
    * @return
    *   cards with normalised eligibiliry values
    */
  private def normaliseCsCards(cards: List[SinglarCSCard]): List[SingleCard] =
    cards.map { card =>
      SingleCard(
        provider = CSCards,
        name = card.cardName,
        APR = card.apr,
        eligibilityRating = NormalisedEligibility.fromCSCards(card.eligibility)
      )
    }

  /** @param cards
    *   the list of cards from CS
    * @return
    *   cards with normalised eligibiliry values
    */
  private def normaliseScoredCards(
      cards: List[SinglarScoredCard]
  ): List[SingleCard] =
    cards.map { card =>
      SingleCard(
        provider = ScoredCards,
        name = card.card,
        APR = card.apr,
        eligibilityRating = NormalisedEligibility.fromScoredCards(
          card.approvalRating
        )
      )
    }

  /** @param fa
    *   the result of a client call
    * @tparam A
    *   the return type for the client call
    * @return
    *   the client call with errors raised
    *
    * This function raises the errors caught in the client layer into the
    * service layer so that they can all be handled together in the
    * HttpErrorMiddleware
    */
  private def catchClientErrors[A](fa: F[Either[CardsClientError, A]]): F[A] = {
    val maybeResult = fa.attempt.map {
      case Left(value) =>
        value match {
          case e: TimeoutException =>
            Left[CreditCardsServiceError, A](RequestTimedOut(e))
          case _ =>
            Left[CreditCardsServiceError, A](UnexpectedServiceError(value))
        }
      case Right(value) => value.leftMap(ErrorFetchingCardDetails)
    }
    EitherT(maybeResult).rethrowT
  }

  private def getCsCards(
      username: Username,
      creditScore: CreditScore
  ): F[List[SingleCard]] = {
    for {
      csCards <- catchClientErrors(
        csCardsClient.getCards(username, creditScore)
      )
      _ <- Logger[F].info("got CS cards, normalizing scores")
      normalizedCSCards = normaliseCsCards(csCards)
    } yield normalizedCSCards
  }

  private def getScoredCards(
      username: Username,
      creditScore: CreditScore,
      salary: Salary
  ): F[List[SingleCard]] =
    for {
      scoredCards <- catchClientErrors(
        scoredCardsClient.getCards(
          username,
          creditScore,
          salary
        )
      )
      _ <- Logger[F].info("got CS cards, normalizing scores")
      normalizedCSCards = normaliseScoredCards(scoredCards)
    } yield normalizedCSCards

  /** @param cards
    *   the cards with normalised eligibility
    * @return
    *   the list of card details with the scores calculated
    */
  private def calculateCardScores(cards: List[SingleCard]): List[CardDetails] =
    cards.map { card =>
      val cardScore = CreditCardsService.singleCardScore(card)
      CardDetails(card.provider, card.name, card.APR, cardScore)
    }

  private def sortCardScores(cards: List[CardDetails]): List[CardDetails] =
    cards.sorted

  /** @param maybeCreditScore
    *   the raw value of the credit score
    * @return
    *   the validated credit score or raise an error to be handled in the
    *   HttpErrorMiddleware
    */
  private def validateCreditScore(
      maybeCreditScore: Int
  ): F[CreditScore] =
    MonadThrow[F]
      .fromOption(
        CreditScoreValue.fromInt(maybeCreditScore),
        InvalidCreditScore
      )
      .map(CreditScore(_))

  override def cardsForUser(
      username: Username,
      rawCreditScore: Int,
      salary: Salary
  ): F[List[CardDetails]] =
    for {
      creditScore <- validateCreditScore(rawCreditScore)
      _           <- Logger[F].info(s"validated creditscore: $creditScore")
      csCards     <- getCsCards(username, creditScore)
      _           <- Logger[F].info(s"Got CS cards and normalized")
      scoredCards <- getScoredCards(username, creditScore, salary)
      _           <- Logger[F].info(s"Got scored cards and normalized")
      cardsWithScores = calculateCardScores(csCards ++ scoredCards)
      _ <- Logger[F].info(s"Calculated card scores")
      details = sortCardScores(cardsWithScores)
    } yield details

}

object CreditCardsService {

  /** @param card
    *   the card whose score is being calcualted
    * @return
    *   the calculated card score
    */
  def singleCardScore(card: SingleCard): CardScore = {
    val rawScore =
      card.eligibilityRating.value * Math.pow(1.0 / card.APR.value, 2)
    // Truncate the score to 3 decimal places - multiply by 10^3, convert to int, divide by 10^3
    val truncatedScore = Math.floor(rawScore * 1000) / 1000
    CardScore(
      truncatedScore
    )
  }

}
