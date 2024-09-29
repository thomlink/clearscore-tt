package creditcards.client

import cats.MonadThrow
import cats.effect.Concurrent
import cats.implicits._
import creditcards.client.model.{
  CSCardsRequest,
  CardsClientError,
  SinglarCSCard
}
import creditcards.service.model.{CreditScore, Username}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}

trait CSCardsClient[F[_]] {

  def getCards(
      name: Username,
      score: CreditScore
  ): F[Either[CardsClientError, List[SinglarCSCard]]]

}

class HttpCSCardsClient[F[_]: Concurrent: MonadThrow](
    client: Client[F],
    baseUri: Uri
) extends CSCardsClient[F] {

  override def getCards(
      name: Username,
      score: CreditScore
  ): F[Either[CardsClientError, List[SinglarCSCard]]] = {
    val requestBody = CSCardsRequest(
      name,
      score
    )

    val request = Request[F](
      uri = baseUri / "cards",
      method = Method.POST
    ).withEntity(requestBody)

    client.run(request).use { resp =>
      // If the status is 200, attempt to decode
      resp.status.code match {
        case 200 =>
          resp
            .as[List[SinglarCSCard]]
            .attempt
            .map(_.leftMap(CardsClientError.DecodingFailure))
        // Otherwise convert it to a domain error to be handled in the layer above
        case 404 =>
          resp
            .as[String]
            .attempt
            .map(msg => Left(CardsClientError.NotFound(msg.toOption)))
        case 400 =>
          resp
            .as[String]
            .attempt
            .map(msg => Left(CardsClientError.BadRequest(msg.toOption)))
        case _ =>
          resp
            .as[String]
            .attempt
            .map(msg => Left(CardsClientError.Other(msg.toOption)))
      }
    }

  }

}
