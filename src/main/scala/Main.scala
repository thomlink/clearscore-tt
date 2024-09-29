import cats.effect.kernel.Resource
import cats.effect.{ExitCode, IO, IOApp}
import creditcards.AppClients
import creditcards.client.{HttpCSCardsClient, HttpScoredCardsClient}
import creditcards.config.AppConfig
import http.{ClientMiddleware, Server}
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

object Main extends IOApp {

  def makeClients(config: AppConfig): Resource[IO, AppClients[IO]] =
    for {
      csCardsClient <- EmberClientBuilder
        .default[IO]
        .build
        .map(ClientMiddleware.apply[IO](_)(config.timeout))

      scoredCardsClient <- EmberClientBuilder
        .default[IO]
        .build
        .map(ClientMiddleware.apply(_)(config.timeout))

      csCardsClientImpl =
        new HttpCSCardsClient[IO](csCardsClient, config.csCardsBaseUri.uri)
      scoredCardsClientImpl =
        new HttpScoredCardsClient[IO](
          scoredCardsClient,
          config.scoredCardsBaseUri.uri
        )

    } yield AppClients(csCardsClientImpl, scoredCardsClientImpl)

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
    implicit val logger: SelfAwareStructuredLogger[IO] =
      LoggerFactory[IO].getLogger

    for {
      config <- AppConfig.read[IO]
      _ <-
        makeClients(config).flatMap { clients =>
          Server.serverResource(config, clients)
        }.useForever

    } yield ExitCode.Success
  }

}
