package creditcards.config

import cats.MonadThrow
import cats.effect.Async
import cats.implicits._
import ciris.{ConfigDecoder, env}
import com.comcast.ip4s.Port
import org.http4s.Uri

case class CsCardsBaseUri(uri: Uri)
case class ScoredCardsBaseUri(uri: Uri)
case class TimeoutSeconds(value: Int)

case class AppConfig(
    port: Port,
    csCardsBaseUri: CsCardsBaseUri,
    scoredCardsBaseUri: ScoredCardsBaseUri,
    timeout: TimeoutSeconds
)

object AppConfig {

  implicit final val UriConfigDecoder: ConfigDecoder[String, Uri] =
    ConfigDecoder[String].mapOption("Uri")(u => Uri.fromString(u).toOption)

  def read[F[_]: Async: MonadThrow]: F[AppConfig] = {
    for {
      port <- env("HTTP_PORT").as[Int].default(8080).load[F].flatMap {
        portNumber =>
          MonadThrow[F].fromOption(
            Port.fromInt(portNumber),
            new Throwable(
              s"Failed to load config, portNumber $portNumber is invalid"
            )
          )
      }
      csCardsBaseUri <- env("CSCARDS_ENDPOINT")
        .as[Uri]
        .map(CsCardsBaseUri)
        .load[F]
      scoredCardsBaseUri <- env("SCOREDCARDS_ENDPOINT")
        .as[Uri]
        .map(ScoredCardsBaseUri)
        .load[F]

      // Hardcoded timeout for now
      timeout = TimeoutSeconds(10)

    } yield AppConfig(
      port,
      csCardsBaseUri,
      scoredCardsBaseUri,
      timeout
    )

  }

}
