package creditcards

import creditcards.client.CardsClientError

package object service {
  sealed trait CreditCardsServiceError extends Throwable

  case class ErrorFetchingCardDetails(e: CardsClientError)
      extends CreditCardsServiceError

  case object InvalidCreditScore extends CreditCardsServiceError {

    def description: String =
      "Credit score value must be between 0 and 700 (inclusively)"

  }

}
