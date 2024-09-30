package creditcards

import creditcards.client.CardsClientError

import scala.concurrent.TimeoutException

package object service {

  // Domain modelled errors for the service layer
  sealed trait CreditCardsServiceError extends Throwable

  case class ErrorFetchingCardDetails(e: CardsClientError)
      extends CreditCardsServiceError

  case class RequestTimedOut(e: TimeoutException)
      extends CreditCardsServiceError

  case object InvalidCreditScore extends CreditCardsServiceError {

    def description: String =
      "Credit score value must be between 0 and 700 (inclusively)"

  }

  case class UnexpectedServiceError(e: Throwable)
      extends CreditCardsServiceError

}
