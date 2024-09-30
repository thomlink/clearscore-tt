package routes

import cats.effect.IO
import creditcards.client
import creditcards.client.model.SinglarScoredCard
import creditcards.client.{CSCardsClient, model}
import creditcards.service.model._
import creditcards.service.{CreditCardsServiceImpl, InvalidCreditScore}
import eu.timepit.refined.auto._
import http.model.CreditCardsRequest
import io.circe.Json
import io.circe.syntax.EncoderOps
import mocks.{MockCreditCardService, MockCsCardsClient, MockScoredCardsClient}
import org.http4s.Status.GatewayTimeout
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import utils._
import utils.Generators._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.concurrent.TimeoutException

object CreditCardRoutesSpec extends SimpleIOSuite with Checkers {

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

    testApp.creditCards(request).map(r => expect(r == expected))
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

    testApp.creditCardsStatusResponse(request).map(_.code).attempt.map {
      case Right(400) => success
      case other =>
        failure(s"Got wrong code from response, expected 504, got $other")
    }

  }

  test("""
         |Routes should
         |return a 504 gateway timeout
         |when the downstream call takes too long
         |""".stripMargin) {

    val timeoutClient =
      new CSCardsClient[IO] {
        override def getCards(
            name: Username,
            score: CreditScore
        ): IO[Either[client.CardsClientError, List[model.SinglarCSCard]]] =
          IO.raiseError(new TimeoutException("Call timed out"))
      }

    val username    = Username("tom")
    val creditScore = CreditScore(100)
    val salary      = Salary(10000)

    val blankScoredCards
        : Map[(Username, CreditScore, Salary), List[SinglarScoredCard]] = Map(
      (username, creditScore, salary) -> Nil
    )

    val scoredCardsClient = new MockScoredCardsClient(blankScoredCards)

    val service = serviceWithClients(
      timeoutClient,
      scoredCardsClient
    )

    val testApp = new TestApp(service)

    val request = CreditCardsRequest(
      username = username,
      creditScore = creditScore.value,
      salary = salary
    )

    testApp.creditCardsStatusResponse(request).map(_.code).attempt.map {
      case Right(504) => success
      case other =>
        failure(s"Got wrong code from response, expected 504, got $other")
    }

  }

  test("""
         |Routes should
         |return a 502 bad gateway
         |when the downstream call fails
         |""".stripMargin) {

    forall(Generators.clientErrorGen) { clientError =>
      val username    = Username("tom")
      val creditScore = CreditScore(100)
      val salary      = Salary(10000)

      val blankScoredCards
          : Map[(Username, CreditScore, Salary), List[SinglarScoredCard]] = Map(
        (username, creditScore, salary) -> Nil
      )

      val scoredCardsClient = new MockScoredCardsClient(blankScoredCards)

      val service = serviceWithClients(
        failingCsClient(clientError),
        scoredCardsClient
      )

      val testApp = new TestApp(service)

      val request = CreditCardsRequest(
        username = username,
        creditScore = creditScore.value,
        salary = salary
      )

      testApp.creditCardsStatusResponse(request).map(_.code).attempt.map {
        case Right(502) => success
        case other =>
          failure(s"Got wrong code from response, expected 502, got $other")
      }
    }
  }

  test("""
         |Routes should
         |return a 400 bad request
         |if the incoming request body cannot be decoded
         |""".stripMargin) {

    val CSCardsClient     = new MockCsCardsClient(Map.empty)
    val ScoredCardsClient = new MockScoredCardsClient(Map.empty)

    val service =
      new CreditCardsServiceImpl[IO](CSCardsClient, ScoredCardsClient)

    val testApp = new TestApp(service)

    val request = Json.obj(
      "foo" -> "bar".asJson
    )

    testApp.creditCardsWithJson(request).map(_.code).attempt.map {
      case Right(400) => success
      case other =>
        failure(s"Got wrong code from response, expected 404, got $other")
    }

  }

}
