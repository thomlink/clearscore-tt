package http

import cats.Applicative
import creditcards.client.CardsClientError
import creditcards.service
import creditcards.service.CreditCardsServiceError
import org.http4s.Response

import scala.concurrent.TimeoutException

object HttpErrorMiddleware {

  def handle[F[_]: Applicative]: PartialFunction[Throwable, F[Response[F]]] = {
    e =>
      object dsl extends org.http4s.dsl.Http4sDsl[F]
      import dsl._

      e match {
        case cse: CreditCardsServiceError =>
          cse match {
            case service.ErrorFetchingCardDetails(clientError) =>
              clientError match {
                case CardsClientError.DecodingFailure(e)        => ???
                case CardsClientError.NotFound(maybeErrorMsg)   => ???
                case CardsClientError.BadRequest(maybeErrorMsg) => ???
                case CardsClientError.Other(maybeErrorMsg)      => ???
              }
            case cse @ service.InvalidCreditScore => BadRequest(cse.description)
          }
        case te: TimeoutException => ???
        case unhandled            => InternalServerError(unhandled.toString)
      }
  }

}
