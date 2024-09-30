package creditcards.client

import creditcards.service.model.{APR, CardName, CreditScore, Salary, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object model {

  /** @param value
    *   eligibility from 0.0 to 10.0
    */
  case class CSEligibility(value: Double)

  object CSEligibility {
    implicit val d: Decoder[CSEligibility] = deriveUnwrappedDecoder
  }

  /** @param value
    *   approval from 0.0 to 1.0
    */
  case class SCApprovalRating(value: Double)

  object SCApprovalRating {
    implicit val d: Decoder[SCApprovalRating] = deriveUnwrappedDecoder
  }

  case class SinglarScoredCard(
      card: CardName,
      apr: APR,
      approvalRating: SCApprovalRating
  )

  object SinglarScoredCard {
    implicit val d: Decoder[SinglarScoredCard] = deriveDecoder
  }

  case class ScoredCardsRequest(
      name: Username,
      score: CreditScore,
      salary: Salary
  )

  object ScoredCardsRequest {
    implicit val e: Encoder[ScoredCardsRequest] = deriveEncoder
  }

  case class SinglarCSCard(
      cardName: CardName,
      apr: APR,
      eligibility: CSEligibility
  )

  object SinglarCSCard {
    implicit val d: Decoder[SinglarCSCard] = deriveDecoder
  }

  case class CSCardsRequest(
      name: Username,
      creditScore: CreditScore
  )

  object CSCardsRequest {
    implicit val e: Encoder[CSCardsRequest] = deriveEncoder
  }

}
