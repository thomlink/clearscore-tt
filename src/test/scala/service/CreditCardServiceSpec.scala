package service

import cats.effect.IO
import creditcards.service.model._
import creditcards.service.{CreditCardsService, CreditCardsServiceImpl}
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import weaver.SimpleIOSuite

object CreditCardServiceSpec extends SimpleIOSuite {

  def createService: CreditCardsService[IO] = {
    implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
    implicit val logger: SelfAwareStructuredLogger[IO] =
      LoggerFactory[IO].getLogger

    new CreditCardsServiceImpl[IO]
  }

  test(
    """
      |The CreditCardsService
      |should return correctly sorted data
      |when the downstream clients return data
      |""".stripMargin
  ) {

    val service = createService

    val expected = List(
      CardDetails(
        CardProvider.CSCards,
        CardName("foo card"),
        APR(10.0),
        CardScore(10)
      )
    )

    val username = Username("tom")
    val creditScore = CreditScore(100)
    val salary = Salary(10000)

    service.cardsForUser(username, creditScore, salary).map{ result =>
      expect(result == expected)
    }
  }

}
