package creditcards.client

import cats.MonadThrow
import cats.effect.Concurrent
import creditcards.client.model.{CSEligibility, CardsClientError, SCApprovalRating, ScoredCardsRequest, SinglarScoredCard}
import creditcards.service.model.{APR, CardName, CreditScore, Salary, Username}
import io.circe.generic.JsonCodec
import org.http4s.{Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.circe.CirceEntityCodec._
import cats.implicits._

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
