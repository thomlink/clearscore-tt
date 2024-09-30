package creditcards.client

import cats.MonadThrow
import cats.effect.Concurrent
import cats.implicits._
import creditcards.client.model.{ScoredCardsRequest, SinglarScoredCard}
import creditcards.service.model.{CreditScore, Salary, Username}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}

trait ScoredCardsClient[F[_]] {

  def getCards(
      name: Username,
      score: CreditScore,
      salary: Salary
  ): F[Either[CardsClientError, List[SinglarScoredCard]]]

}

class HttpScoredCardsClient[F[_]: Concurrent: MonadThrow](
    client: Client[F],
    baseUri: Uri
) extends ScoredCardsClient[F] {

  override def getCards(
      name: Username,
      score: CreditScore,
      salary: Salary
  ): F[Either[CardsClientError, List[SinglarScoredCard]]] = {
    val requestBody = ScoredCardsRequest(
      name,
      score,
      salary
    )

    val request = Request[F](
      uri = baseUri / "creditcards",
      method = Method.POST
    ).withEntity(requestBody)

    client.run(request).use { resp =>
      // If the status is 200, attempt to decode
      // Could be moved to a shared object, as both clients use the same logic
      // Timeout errors raised as TimeoutExceptions, so not handled here
      resp.status.code match {
        case 200 =>
          resp
            .as[List[SinglarScoredCard]]
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
