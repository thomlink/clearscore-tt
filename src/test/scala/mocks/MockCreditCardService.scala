package mocks

import cats.effect.IO
import creditcards.service.model._
import creditcards.service.{CreditCardsService, model}

class MockCreditCardService(
    mockData: Map[(Username, Int, Salary), List[CardDetails]]
) extends CreditCardsService[IO] {

  override def cardsForUser(
      username: model.Username,
      creditScore: Int,
      salary: model.Salary
  ): IO[List[model.CardDetails]] =
    mockData.get((username, creditScore, salary)) match {
      case Some(value) => IO.pure(value)
      case None        => IO.raiseError(new Throwable("Not found in test data"))
    }

}
