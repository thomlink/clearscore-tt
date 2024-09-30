import cats.effect.IO
import creditcards.client.{CSCardsClient, CardsClientError, ScoredCardsClient}
import creditcards.client.model.{SinglarCSCard, SinglarScoredCard}
import creditcards.service.{CreditCardsService, CreditCardsServiceImpl}
import creditcards.service.model.{CreditScore, Salary, Username}
import mocks.{MockCsCardsClient, MockScoredCardsClient}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import service.CreditCardServiceSpec.{failure, success}
import weaver.Expectations

package object utils {

  def failingCsClient(e: CardsClientError): CSCardsClient[IO] =
    new CSCardsClient[IO] {

      override def getCards(
          name: Username,
          score: CreditScore
      ): IO[Either[CardsClientError, List[SinglarCSCard]]] = IO.pure(Left(e))

    }

  def passIfExpectedError[A, E <: Throwable](
      maybeError: Either[Throwable, A],
      expectedError: E
  ): Expectations =
    maybeError match {
      case Right(_) => failure("Did not fail")
      case Left(received) =>
        if (received == expectedError)
          success
        else
          failure(
            s"Did not fail correctlty - Expected $expectedError, found $received"
          )

    }

  case class ClientData(
      csCardResults: Map[(Username, CreditScore), List[SinglarCSCard]] =
        Map.empty,
      scoredCardResults: Map[(Username, CreditScore, Salary), List[
        SinglarScoredCard
      ]] = Map.empty
  )

  def serviceWithClients(
      csCardsClient: CSCardsClient[IO],
      scCardsClient: ScoredCardsClient[IO]
  ): CreditCardsService[IO] = {
    implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
    implicit val logger: SelfAwareStructuredLogger[IO] =
      LoggerFactory[IO].getLogger
    new CreditCardsServiceImpl[IO](csCardsClient, scCardsClient)
  }

  def serviceWithData(clientData: ClientData): CreditCardsService[IO] = {
    val csCardsClient = new MockCsCardsClient(clientData.csCardResults)
    val scoredCardsClient =
      new MockScoredCardsClient(clientData.scoredCardResults)

    serviceWithClients(csCardsClient, scoredCardsClient)
  }

}
