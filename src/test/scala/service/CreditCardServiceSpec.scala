package service

import creditcards.client.model.{CSEligibility, SCApprovalRating, SinglarCSCard, SinglarScoredCard}
import creditcards.service.model._
import creditcards.service.{ErrorFetchingCardDetails, InvalidCreditScore}
import eu.timepit.refined.auto._
import mocks.MockScoredCardsClient
import utils.Generators._
import utils._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object CreditCardServiceSpec extends SimpleIOSuite with Checkers {

  test(
    """
      |The CreditCardsService
      |should return correctly sorted and scored data
      |when the downstream clients return data
      |""".stripMargin
  ) {

    val username    = Username("tom")
    val creditScore = CreditScore(100)
    val salary      = Salary(10000)

    val testCsCards: Map[(Username, CreditScore), List[SinglarCSCard]] = Map(
      (username, creditScore) -> List(
        SinglarCSCard(
          cardName = CardName("SuperSaver Card"),
          apr = APR(21.4),
          eligibility = CSEligibility(6.3)
        )
      )
    )

    val testScoredCards
        : Map[(Username, CreditScore, Salary), List[SinglarScoredCard]] = Map(
      (username, creditScore, salary) -> List(
        SinglarScoredCard(
          card = CardName("ScoredCard Builder"),
          apr = APR(19.4),
          approvalRating = SCApprovalRating(0.8)
        )
      )
    )

    val clientData = ClientData(
      testCsCards,
      testScoredCards
    )

    val service = serviceWithData(clientData)

    val expected = List(
      CardDetails(
        CardProvider.ScoredCards,
        CardName("ScoredCard Builder"),
        APR(19.4),
        CardScore(0.212)
      ),
      CardDetails(
        CardProvider.CSCards,
        CardName("SuperSaver Card"),
        APR(21.4),
        CardScore(0.137)
      )
    )

    service.cardsForUser(username, creditScore.value, salary).map { result =>
      expect(result == expected)
    }
  }

  test(
    """
      |The CreditCardsService
      |should catch client errors
      |and raise them as service errors
      |""".stripMargin
  ) {

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

      service.cardsForUser(username, creditScore.value, salary).attempt.map {
        maybeError =>
          passIfExpectedError(maybeError, ErrorFetchingCardDetails(clientError))
      }
    //      service.cardsForUser(username, creditScore.value, salary).attempt.map {
    //        case Right(cs) => failure(s"Expected error, got $cs")
    //        case Left(ErrorFetchingCardDetails(ce)) => expect(ce == clientError)
    //        case Left(other) =>
    //          failure(
    //            s"Didn't fail correctly. Expected ErrorFetchingCardDetails got $other"
    //          )
    //      }
    //
    }
  }

  test(
    """
      |The CreditCardsService
      |should raise the appropriate error
      |when an invalid creditscore is provided
      |""".stripMargin
  ) {

    val service = serviceWithData(ClientData())

    val username    = Username("tom")
    val creditScore = 780 // Invalid, should be 0-700
    val salary      = Salary(10000)

    service.cardsForUser(username, creditScore, salary).attempt.map { feither =>
      passIfExpectedError(feither, InvalidCreditScore)
    }

//    service.cardsForUser(username, creditScore, salary).attempt.map {
//      case Right(cs)                => failure(s"Expected error, got $cs")
//      case Left(InvalidCreditScore) => success
//      case Left(other) =>
//        failure(
//          s"Didn't fail correctly. Expected ErrorFetchingCardDetails got $other"
//        )
//    }

  }

}
