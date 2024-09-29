package creditcards

package object client {

  sealed trait CardsClientError extends Throwable {
    def description: String
  }

  object CardsClientError {

    case class DecodingFailure(e: Throwable) extends CardsClientError {
      override def description: String = s"Could not decode json: ${e.toString}"
    }

    case class NotFound(maybeErrorMsg: Option[String])
        extends CardsClientError {

      def description: String =
        s"Card client returned a not found error: $maybeErrorMsg"

    }

    case class BadRequest(maybeErrorMsg: Option[String])
        extends CardsClientError {

      def description: String =
        s"Card client returned a badrequest error: $maybeErrorMsg"

    }

    case class Other(maybeErrorMsg: Option[String]) extends CardsClientError {

      def description: String =
        s"Card client an unexpected error: $maybeErrorMsg"

    }

  }

}
