package routes

import cats.effect.IO
import creditcards.service.{CreditCardsServiceImpl, InvalidCreditScore}
import creditcards.service.model._
import http.model.CreditCardsRequest
import mocks.{MockCreditCardService, MockCsCardsClient, MockScoredCardsClient}
import weaver.SimpleIOSuite
import eu.timepit.refined.auto._
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{
  Logger,
  LoggerFactory,
  SelfAwareStructuredLogger,
  StructuredLogger
}

object CreditCardRoutesSpec extends SimpleIOSuite {

  // TODO other tests
  // service error(s)
  // client error(s)
  //    - timeout

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  implicit val logger: SelfAwareStructuredLogger[IO] =
    LoggerFactory[IO].getLogger

  test(
    "Routes should" +
      " correctly return data collated by the CreditCardsService" +
      " when receiving a valid request"
  ) {

    val request = CreditCardsRequest(
      username = Username("abc123"),
      creditScore = 100,
      salary = Salary(50000)
    )

    val expected = List(
      CardDetails(
        CardProvider.CSCards,
        CardName("card1"),
        APR(10.0),
        CardScore(10)
      )
    )

    val mockData = Map(
      (request.username, request.creditScore, request.salary) -> expected
    )

    val stubService = new MockCreditCardService(mockData)

    val testApp = new TestApp(stubService)

    testApp.creditCards(request).map(r => assert(r == expected))
  }

  test("""
      |Routes should
      |return a 400 bad request
      |after it is raised in the credit cards service
      |when the request includes an invalid credit score
      |""".stripMargin) {

    val CSCardsClient     = new MockCsCardsClient(Map.empty)
    val ScoredCardsClient = new MockScoredCardsClient(Map.empty)

    val service =
      new CreditCardsServiceImpl[IO](CSCardsClient, ScoredCardsClient)

    val testApp = new TestApp(service)

    val request = CreditCardsRequest(
      username = Username("abc123"),
      creditScore = 701,
      salary = Salary(50000)
    )

    testApp.creditCards(request).attempt.map {
      case Left(e) =>
        e match {
          case InvalidCreditScore => success
          case other =>
            failure(
              s"wrong error returned, Expected InvalidCreditScore, got $other"
            )
        }
      case _ => failure("Did not fail, got a valid response")
    }
  }

}
