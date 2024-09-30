package http

import cats.effect._
import cats.implicits._
import cats.{Applicative, Monad, MonadThrow}
import creditcards.service.CreditCardsService
import creditcards.service.model.CardDetails
import http.model.CreditCardsRequest.decoder
import http.model.{CreditCardsRequest, DecodeIncomingBodyFailure}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, _}

object Routes {

  def creditCards[F[_]: MonadThrow: Concurrent: Monad](
      service: CreditCardsService[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    implicit val creditCardsRequestDecoder
        : EntityDecoder[F, CreditCardsRequest] = jsonOf[F, CreditCardsRequest]

    implicit val cardDetailsEncoder: EntityEncoder[F, CardDetails] =
      jsonEncoderOf[F, CardDetails]

    val routes = HttpRoutes.of[F] {
      case req @ POST -> Root / "creditcards" =>
        for {
          // First try to decode the message body. If it fails, raise the appropriate error
          // to be handlded in the HttpErrorMiddleware
          ccRequest <- req.as[CreditCardsRequest].attempt.flatMap {
            case Left(value) =>
              MonadThrow[F].raiseError[CreditCardsRequest](
                DecodeIncomingBodyFailure(value)
              )
            case Right(request) => Applicative[F].pure(request)
          }
          result <- service
            .cardsForUser(
              ccRequest.username,
              ccRequest.creditScore,
              ccRequest.salary
            )
            .flatMap(Ok(_))
        } yield result
    }

    HttpErrorMiddleware(routes)
  }

}
