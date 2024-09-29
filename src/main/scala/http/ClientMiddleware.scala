package http

import cats.effect.implicits.genTemporalOps
import cats.effect.{Sync, Temporal}
import org.http4s.{Header, Headers}
import org.http4s.client.Client
import org.typelevel.ci.CIStringSyntax

import scala.concurrent.duration.FiniteDuration

object ClientMiddleware {

  def withUserAgentHeader[F[_]: Sync]: Client[F] => Client[F] = { client =>
    Client { req =>
      client.run(
        req.withHeaders(
          req.headers ++ Headers(Header.Raw(ci"User-Agent", "TechTestApi"))
        )
      )
    }
  }

  def withTimeout[F[_]: Temporal](
      timeout: FiniteDuration
  ): Client[F] => Client[F] = { client =>
    Client { req =>
      client.run(req).timeout(timeout)
    }
  }

  def apply[F[_]: Sync](client: Client[F])(timeout: FiniteDuration): Client[F] =
    withTimeout(timeout)(withUserAgentHeader(client))

}
