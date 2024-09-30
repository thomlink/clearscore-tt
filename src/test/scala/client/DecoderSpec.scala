package client

import creditcards.client.model.{CSEligibility, SCApprovalRating, SinglarCSCard, SinglarScoredCard}
import creditcards.service.model.{APR, CardName}
import io.circe.parser.parse
import weaver.SimpleIOSuite

object DecoderSpec extends SimpleIOSuite {

  pureTest("""
      |The scored cards client
      |should decode the response
      |into a List[SingularScoredCard]
      |when the response is valid
      |""".stripMargin) {

    val responseString =
      """
        |[
        |   {
        |     "card": "ScoredCard Builder",
        |     "apr": 19.4,
        |     "approvalRating": 0.8
        |   }
        |]
        |""".stripMargin

    val json = parse(responseString).toOption.get

    val result = json.as[List[SinglarScoredCard]]

    val expected = Right(
      List(
        SinglarScoredCard(
          card = CardName("ScoredCard Builder"),
          apr = APR(19.4),
          approvalRating = SCApprovalRating(0.8)
        )
      )
    )

    expect(result == expected)

  }

  pureTest("""
             |The CS cards client
             |should decode the response
             |into a List[SinglarCSCard]
             |when the response is valid
             |""".stripMargin) {

    val responseString =
      """
        |[
        |   {
        |     "cardName": "SuperSaver Card",
        |     "apr": 21.4,
        |     "eligibility": 6.3
        |   },
        |   {
        |     "cardName": "SuperSpender Card",
        |     "apr": 19.2,
        |     "eligibility": 5.0
        |   }
        |]
        |""".stripMargin

    val json = parse(responseString).toOption.get

    val result = json.as[List[SinglarCSCard]]

    val expected = Right(
      List(
        SinglarCSCard(
          cardName = CardName("SuperSaver Card"),
          apr = APR(21.4),
          eligibility = CSEligibility(6.3)
        ),
        SinglarCSCard(
          cardName = CardName("SuperSpender Card"),
          apr = APR(19.2),
          eligibility = CSEligibility(5.0)
        )
      )
    )
    expect(result == expected)
  }

}
