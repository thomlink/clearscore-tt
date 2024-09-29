package creditcards.client

import creditcards.service.model.{APR, CardName, CreditScore, Salary, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object model {

  case class CSEligibility(value: Double)

  object CSEligibility {
    implicit val d: Decoder[CSEligibility] = deriveUnwrappedDecoder
  }

  case class SCApprovalRating(value: Double)

  object SCApprovalRating {
    implicit val d: Decoder[SCApprovalRating] = deriveUnwrappedDecoder
  }



//  @JsonCodec(decodeOnly = true)
  case class SinglarScoredCard(
                                card: CardName,
                                apr: APR,
                                approvalRating: SCApprovalRating
  )

  object SinglarScoredCard {
    implicit val d: Decoder[SinglarScoredCard] = deriveDecoder
  }

//  @JsonCodec(encodeOnly = true)
  case class ScoredCardsRequest(
      name: Username,
      score: CreditScore,
      salary: Salary
  )

  object ScoredCardsRequest {
    implicit val e: Encoder[ScoredCardsRequest] = deriveEncoder
  }

//  @JsonCodec(decodeOnly = true)
  case class SinglarCSCard(
      cardName: CardName,
      apr: APR,
      eligibility: CSEligibility
  )

  object SinglarCSCard {
    implicit val d: Decoder[SinglarCSCard] = deriveDecoder
  }

//  @JsonCodec(encodeOnly = true)
  case class CSCardsRequest(
      name: Username,
      score: CreditScore
  )

  object CSCardsRequest {
    implicit val e: Encoder[CSCardsRequest] = deriveEncoder
  }

}
