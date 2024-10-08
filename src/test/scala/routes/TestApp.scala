package routes

import cats.effect.IO
import creditcards.service.CreditCardsService
import creditcards.service.model.CardDetails
import http.Routes
import http.model.CreditCardsRequest
import io.circe.Json
import org.http4s.Method.POST
import org.http4s.{Request, Status}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.implicits._
import routes.TestCodecs._

class TestApp(
    service: CreditCardsService[IO]
) {

  val routes                 = Routes.creditCards(service)
  val httpClient: Client[IO] = Client.fromHttpApp[IO](routes.orNotFound)

  def creditCards(request: CreditCardsRequest): IO[List[CardDetails]] = {

    httpClient
      .run(
        Request[IO](
          method = POST,
          uri = uri"/creditcards"
        ).withEntity(request)
      )
      .use(resp => resp.as[List[CardDetails]])
  }

  def creditCardsWithJson(json: Json): IO[Status] =
    httpClient
      .status(
        Request[IO](
          method = POST,
          uri = uri"/creditcards"
        ).withEntity(json)
      )

  def creditCardsStatusResponse(request: CreditCardsRequest): IO[Status] = {

    httpClient
      .status(
        Request[IO](
          method = POST,
          uri = uri"/creditcards"
        ).withEntity(request)
      )

  }

}
