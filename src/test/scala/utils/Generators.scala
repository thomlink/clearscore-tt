package utils

import cats.Show
import creditcards.client.CardsClientError
import org.scalacheck.Gen

object Generators {

  val clientErrorGen: Gen[CardsClientError] = Gen.oneOf(
    List(
      CardsClientError.Other(None),
      CardsClientError.NotFound(None),
      CardsClientError.BadRequest(None),
      CardsClientError.DecodingFailure(new Throwable("decoding failure"))
    )
  )

  implicit val showCE: Show[CardsClientError] =
    new Show[CardsClientError] {
      override def show(t: CardsClientError): String = t.description
    }

}
