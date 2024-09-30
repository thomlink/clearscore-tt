package http

import cats.effect.{Sync, Temporal}
import org.http4s.client.Client
import org.http4s.{Header, Headers}
import org.typelevel.ci.CIStringSyntax

object ClientMiddleware {

  /** @param client
    * @return
    *   a client which attches a user agent header to every request
    */
  def withUserAgentHeader[F[_]: Sync](client: Client[F]): Client[F] = {
    Client { req =>
      client.run(
        req.withHeaders(
          req.headers ++ Headers(
            Header.Raw(
              ci"User-Agent",
              "TechTestApi"
            ) // Value should probably be stored in config, but for a tech test here is fine
          )
        )
      )
    }
  }

  /**
   * @param client
   * @param timeoutSeconds the timout to allow for every request
   * @return a client which attached the keep alive header to every request.
   *         Requests which take more than 10s to respone will raise TimeoutException errors
   */
  def withTimeoutHeader[F[_]: Temporal](
      client: Client[F],
      timeoutSeconds: Int
  ): Client[F] = {
    Client { req =>
      client.run(
        req.withHeaders(
          req.headers ++ Headers(
            Header.Raw(ci"Keep-Alive", s"timeout=$timeoutSeconds")
          )
        )
      )
    }
  }

  def apply[F[_]: Sync: Temporal](client: Client[F])(
      timeoutSeconds: Int
  ): Client[F] = withUserAgentHeader(withTimeoutHeader(client, timeoutSeconds))

}
