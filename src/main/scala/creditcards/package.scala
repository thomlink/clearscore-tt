import creditcards.client.{CSCardsClient, ScoredCardsClient}

package object creditcards {

  case class AppClients[F[_]](
      csClient: CSCardsClient[F],
      scClient: ScoredCardsClient[F]
  )

}
