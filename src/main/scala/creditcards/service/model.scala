package creditcards.service

import creditcards.client.model.{CSEligibility, SCApprovalRating}
import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.api.{RefType, Refined}
import eu.timepit.refined.numeric.Interval
import io.circe.generic.extras.semiauto.{deriveUnwrappedDecoder, deriveUnwrappedEncoder}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.refined.refinedDecoder
import io.circe.{Decoder, Encoder}

object model {

  case class Username(value: String)

  object Username {
    implicit val d: Decoder[Username] = deriveUnwrappedDecoder
    implicit val e: Encoder[Username] = deriveUnwrappedEncoder
  }

  // Refined type as a credit score by definition can only be between 0 and 700
  type CreditScoreValue = Int Refined Interval.Closed[0, 700]

  object CreditScoreValue {

    def fromInt(i: Int): Option[CreditScoreValue] =
      RefType.applyRef[CreditScoreValue](i).toOption

    implicit val d: Decoder[CreditScoreValue] = refinedDecoder
    implicit val e: Encoder[CreditScoreValue] = Encoder[Int].contramap(_.value)

  }

  case class CreditScore(value: CreditScoreValue)

  object CreditScore {
    implicit val d: Decoder[CreditScore] = deriveUnwrappedDecoder
    implicit val e: Encoder[CreditScore] = Encoder[Int].contramap(_.value)

  }

  case class Salary(value: Int)

  object Salary {
    implicit val d: Decoder[Salary] = deriveUnwrappedDecoder
    implicit val e: Encoder[Salary] = deriveUnwrappedEncoder
  }

  /** @param provider
    *   the card provider
    * @param name
    *   the card name
    * @param apr
    *   the apr of the card
    * @param cardScore
    *   the overall score of the card, used for sorting
    */
  case class CardDetails(
      provider: CardProvider,
      name: CardName,
      apr: APR,
      cardScore: CardScore
  )

  object CardDetails {
    implicit val e: Encoder[CardDetails] = deriveEncoder

    // For the .sorted function
    implicit val ordering: Ordering[CardDetails] =
      new Ordering[CardDetails] {

        /*
        negative if x < y
        positive if x > y
        zero otherwise (if x == y)
         */
        override def compare(x: CardDetails, y: CardDetails): Int = {
          if (x.cardScore.value < y.cardScore.value) {
            1
          } else if (x.cardScore.value > y.cardScore.value)
            -1
          else
            0
        }

      }

  }

  sealed trait CardProvider extends EnumEntry

  object CardProvider extends Enum[CardProvider] with CirceEnum[CardProvider] {

    case object CSCards extends CardProvider

    case object ScoredCards extends CardProvider

    override def values: IndexedSeq[CardProvider] = findValues
  }

  case class CardName(value: String)

  object CardName {
    implicit val e: Encoder[CardName] = deriveUnwrappedEncoder
    implicit val d: Decoder[CardName] = deriveUnwrappedDecoder
  }

  case class APR(value: Double)

  object APR {
    implicit val e: Encoder[APR] = deriveUnwrappedEncoder
    implicit val d: Decoder[APR] = deriveUnwrappedDecoder
  }

  case class CardScore(value: Double)

  object CardScore {
    implicit val e: Encoder[CardScore] = deriveUnwrappedEncoder
  }

  // Keep the eligibility consistent despite different values from card providers
  case class NormalisedEligibility(value: Double)

  object NormalisedEligibility {

    def fromCSCards: CSEligibility => NormalisedEligibility = {
      case eligibility => NormalisedEligibility(eligibility.value * 10.0)
    }

    def fromScoredCards: SCApprovalRating => NormalisedEligibility = {
      case eligibility => NormalisedEligibility(eligibility.value * 100.0)
    }

  }

  case class SingleCard(
      provider: CardProvider,
      name: CardName,
      APR: APR,
      eligibilityRating: NormalisedEligibility
  )

}
