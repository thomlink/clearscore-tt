package http

import cats.effect._
import cats.implicits._
import cats.{Monad, MonadThrow}
import creditcards.service.CreditCardsService
import creditcards.service.model.CardDetails
import http.model.CreditCardsRequest
import http.model.CreditCardsRequest.decoder
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, _}
import org.http4s.EntityEncoder._

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

    HttpRoutes.of[F] {
      case req @ POST -> Root / "creditcards" =>
        for {
          ccRequest <- req.as[CreditCardsRequest]
          result <- service
            .cardsForUser(
              ccRequest.username,
              ccRequest.creditScore,
              ccRequest.salary
            )
            .flatMap(Ok(_))
        } yield result
      case GET -> Root / "ping" => Ok("pong")

    }

  }

//  def creditCardsIO(
//      service: CreditCardsService[IO]
//  ): HttpRoutes[IO] = {
//    val dsl = new Http4sDsl[IO] {}
//    import dsl._
//
//    implicit val creditCardsRequestDecoder
//        : EntityDecoder[IO, CreditCardsRequest] = jsonOf[IO, CreditCardsRequest]
//
//    HttpRoutes.of[IO] { case req @ POST -> Root / "creditcards" =>
//      for {
//        ccRequest <- req.as[CreditCardsRequest]
//        result <- service
//          .cardsForUser(
//            ccRequest.username,
//            ccRequest.creditScore,
//            ccRequest.salary
//          )
//          .flatMap(Ok(_))
//      } yield result
//
//    }
//
//  }

}
