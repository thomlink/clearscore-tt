package http

import cats.effect.{Async, IO, Resource}
import com.comcast.ip4s.{IpLiteralSyntax, Port}
import creditcards.AppClients
import creditcards.config.AppConfig
import creditcards.service.CreditCardsServiceImpl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.{HttpRoutes, Response}
import org.typelevel.log4cats.Logger

object Server {

  def serve[F[_]: Async](
      routes: HttpRoutes[F],
      errorHandler: PartialFunction[Throwable, F[Response[F]]],
      port: Port
  ): Resource[F, Server] =
    EmberServerBuilder.default
      .withHost(ipv4"0.0.0.0")
      .withPort(port)
      .withErrorHandler(errorHandler)
      .withHttpApp(routes.orNotFound)
      .build

  def serverResource(
      config: AppConfig,
      clients: AppClients[IO]
  )(implicit l: Logger[IO]): Resource[IO, Server] = {

    val service =
      new CreditCardsServiceImpl[IO](
        clients.csClient,
        clients.scClient
      )

    val routes = Routes.creditCards[IO](service)

    Server.serve(
      routes,
      HttpErrorMiddleware.handle[IO],
      config.port
    )
  }

}
