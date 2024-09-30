package service

import creditcards.client.model.{CSEligibility, SCApprovalRating}
import creditcards.service.CreditCardsService
import creditcards.service.model.CardProvider.{CSCards, ScoredCards}
import creditcards.service.model.{
  APR,
  CardName,
  CardScore,
  NormalisedEligibility,
  SingleCard
}
import weaver.SimpleIOSuite

object CreditCardServiceUnitTestSpec extends SimpleIOSuite {

  pureTest("""
      |Credit cards service should
      |correctly calculate the card score
      |and should normalise the eligibility correctly
      |""".stripMargin) {

    /* From assignment

      "cardName": "SuperSpender Card",
      "apr": 19.2,
      "eligibility": 5.0


      "card": "ScoredCard Builder",
      "apr": 19.4,
      "approvalRating": 0.8


      "provider": "ScoredCards"
      "name": "ScoredCard Builder",
      "apr": 19.4,
      "cardScore": 0.212

      "provider": "CSCards",
      "name": "SuperSpender Card",
      "apr": 19.2,
      "cardScore": 0.135

     */

    val cscard: SingleCard = SingleCard(
      CSCards,
      CardName("SuperSpender Card"),
      APR(19.2),
      NormalisedEligibility.fromCSCards(CSEligibility(5.0))
    )

    val scoredCard: SingleCard = SingleCard(
      ScoredCards,
      CardName("ScoredCard Builder"),
      APR(19.4),
      NormalisedEligibility.fromScoredCards(SCApprovalRating(0.8))
    )

    expect(
      CreditCardsService.singleCardScore(cscard) == CardScore(0.135) &&
        CreditCardsService.singleCardScore(scoredCard) == CardScore(0.212)
    )

  }

}
