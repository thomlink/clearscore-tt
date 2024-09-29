package mocks

import cats.effect.IO
import creditcards.client.model.CardsClientError.NotFound
import creditcards.client.model.SinglarCSCard
import creditcards.client.{CSCardsClient, model}
import creditcards.service.model.{CreditScore, Username}

class MockCsCardsClient(data: Map[(Username, CreditScore), List[SinglarCSCard]])
    extends CSCardsClient[IO] {

  override def getCards(
      name: Username,
      score: CreditScore
  ): IO[Either[model.CardsClientError, List[SinglarCSCard]]] =
    IO.pure(
      data
        .get(name, score)
        .toRight(
          NotFound(None)
        )
    )

}
