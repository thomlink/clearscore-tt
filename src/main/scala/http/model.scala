package http

import creditcards.service.model.{Salary, Username}
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

object model {

  case class CreditCardsRequest(
      username: Username,
      creditScore: Int, // Validation happens in the service
      salary: Salary
  )

  object CreditCardsRequest {

    implicit val decoder: Decoder[CreditCardsRequest] =
      deriveDecoder[CreditCardsRequest]

  }

  case class DecodeIncomingBodyFailure(e: Throwable) extends Throwable

}
