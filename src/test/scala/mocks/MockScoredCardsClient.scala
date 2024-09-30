package mocks

import cats.effect.IO
import creditcards.client.CardsClientError.NotFound
import creditcards.client.{CardsClientError, ScoredCardsClient}
import creditcards.client.model._
import creditcards.service.model._

class MockScoredCardsClient(
    mockData: Map[(Username, CreditScore, Salary), List[SinglarScoredCard]]
) extends ScoredCardsClient[IO] {

  override def getCards(
      name: Username,
      score: CreditScore,
      salary: Salary
  ): IO[Either[CardsClientError, List[SinglarScoredCard]]] =
    IO.pure(
      mockData.get((name, score, salary)).toRight(NotFound(None))
    )

}
