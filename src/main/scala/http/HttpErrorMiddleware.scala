package http

import cats.{Monad, MonadThrow}
import cats.data.Kleisli
import cats.implicits._
import creditcards.service
import creditcards.service.{
  CreditCardsServiceError,
  RequestTimedOut,
  UnexpectedServiceError
}
import http.model.DecodeIncomingBodyFailure
import org.http4s.HttpRoutes

object HttpErrorMiddleware {

  def apply[F[_]: MonadThrow](routes: HttpRoutes[F]): HttpRoutes[F] = {
    object dsl extends org.http4s.dsl.Http4sDsl[F]
    import dsl._

    /*
    Middleware for the routes run requests and catch domain errors
    All errors we should see at this level are Service level errors and failures decoding the request
    They are all returned with appropriate HTTP error code
     */
    Kleisli { req =>
      routes.run(req).attempt.semiflatMap {
        case Right(response) => Monad[F].pure(response)
        case Left(error) =>
          error match {
            case cse: CreditCardsServiceError =>
              cse match {
                case service.ErrorFetchingCardDetails(clientError) =>
                  BadGateway(
                    s"An error occurred fetching downstream data. Details: ${clientError.description}"
                  )
                case e: service.InvalidCreditScore.type =>
                  BadRequest(e.description)
                case RequestTimedOut(te) =>
                  GatewayTimeout(
                    s"Downstream call took too long to respond, ${te.toString}"
                  )
                case UnexpectedServiceError(error) =>
                  InternalServerError(s"An unexpected error occured, $error")
              }
            case DecodeIncomingBodyFailure(e) =>
              BadRequest(s"Could not decode incoming messaage request. $e")
            case unhandled =>
              InternalServerError(s"An unexpected error occured, $unhandled")
          }
      }
    }

  }

}
