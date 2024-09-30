package routes

import cats.effect.IO
import creditcards.service.model._
import http.model.CreditCardsRequest
import io.circe.generic.extras.semiauto.{
  deriveUnwrappedDecoder,
  deriveUnwrappedEncoder
}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

object TestCodecs {

  implicit val e: Encoder[CreditCardsRequest] = deriveEncoder
  implicit val eUsername: Encoder[Username]   = deriveUnwrappedEncoder
  implicit val eSalary: Encoder[Salary] = deriveUnwrappedEncoder

  implicit val dcn: Decoder[CardName]  = deriveUnwrappedDecoder
  implicit val dapr: Decoder[APR]      = deriveUnwrappedDecoder
  implicit val dcs: Decoder[CardScore] = deriveUnwrappedDecoder
  implicit val d: Decoder[CardDetails] = deriveDecoder

  implicit val entityDecoderCardDetails: EntityDecoder[IO, CardDetails] =
    jsonOf[IO, CardDetails]

  implicit val entityEncoderCCR: EntityEncoder[IO, CreditCardsRequest] =
    jsonEncoderOf[IO, CreditCardsRequest]

}
