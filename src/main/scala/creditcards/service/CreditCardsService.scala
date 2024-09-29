package creditcards.service

import cats.MonadThrow
import cats.data.EitherT
import cats.implicits._
import creditcards.client.model.{SinglarCSCard, SinglarScoredCard}
import creditcards.client.{CSCardsClient, CardsClientError, ScoredCardsClient}
import creditcards.service.model.CardProvider.{CSCards, ScoredCards}
import creditcards.service.model._
import org.typelevel.log4cats.Logger

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

  private def normaliseCsCards(cards: List[SinglarCSCard]): List[SingleCard] =
    cards.map { card =>
      SingleCard(
        provider = CSCards,
        name = card.cardName,
        APR = card.apr,
        eligibilityRating = NormalisedEligibility.fromCSCards(card.eligibility)
      )
    }

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

  private def catchClientErrors[A](fa: F[Either[CardsClientError, A]]): F[A] =
    EitherT(fa)
      .leftMap(ErrorFetchingCardDetails)
      .rethrowT

  private def getCsCards(
      username: Username,
      creditScore: CreditScore
  ): F[List[SingleCard]] = {
    for {
      csCards <- catchClientErrors(
        csCardsClient.getCards(username, creditScore)
      )
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
      normalizedCSCards = normaliseScoredCards(scoredCards)
    } yield normalizedCSCards

  private def calculateCardScores(cards: List[SingleCard]): List[CardDetails] =
    cards.map { card =>
      val cardScore = CreditCardsService.singleCardScore(card)
      CardDetails(card.provider, card.name, card.APR, cardScore)
    }

  private def sortCardScores(cards: List[CardDetails]): List[CardDetails] =
    cards.sorted;

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
      csCards     <- getCsCards(username, creditScore)
      scoredCards <- getScoredCards(username, creditScore, salary)
      cardsWithScores = calculateCardScores(csCards ++ scoredCards)
      details         = sortCardScores(cardsWithScores)
    } yield details

}

object CreditCardsService {

  def singleCardScore(card: SingleCard): CardScore = {
    CardScore(
      card.eligibilityRating.value * Math.pow(1.0 / card.APR.value, 2)
    )
  }

}
