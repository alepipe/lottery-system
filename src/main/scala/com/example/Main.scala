package com.example

import cats.effect.std.Env
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import com.comcast.ip4s._
import com.example.repository.LotterySystemRepository
import com.example.services.LotterySystemService
import doobie.util.transactor.Transactor
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import java.util.UUID
import scala.util.Try

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    def getApiKey: IO[UUID] =
      for {
        maybeApyKey <- Env[IO].get("API_KEY")
        apiKey <- IO
          .fromTry[UUID](Try(maybeApyKey.map(UUID.fromString).get))
          .orElse(for {
            uuid <- IO.randomUUID
            _ <- IO.println(
              "Error looking for the API_KEY, generating a one time one!"
            )
            _ <- IO.println(
              s"Use $uuid to identify yourself as Admin for this session"
            )
          } yield uuid)
      } yield apiKey

    def getPort: IO[Port] =
      for {
        maybePort <- Env[IO].get("HTTP_PORT")
        port <- IO.pure(
          maybePort
            .flatMap(_.toIntOption)
            .flatMap(Port.fromInt)
            .getOrElse(port"8080")
        )
      } yield port

    def getPostgresCredential: IO[(String, String)] =
      for {
        maybePostgresUser     <- Env[IO].get("POSTGRES_USER")
        maybePostgresPassword <- Env[IO].get("POSTGRES_PASSWORD")
        user <- IO.fromOption(maybePostgresUser)(
          new Error("There is no POSTGRES_USER")
        )
        password <- IO.fromOption(maybePostgresPassword)(
          new Error("There is no POSTGRES_PASSWORD")
        )
      } yield (user, password)

    for {
      apiKey <- getApiKey
      port   <- getPort
      transactor <- getPostgresCredential.map { case (user, password) =>
        Transactor.fromDriverManager[IO](
          driver = "org.postgresql.Driver",
          url = s"jdbc:postgresql:$user",
          user = user,
          password = password,
          logHandler = None
        )
      }
      repository      = LotterySystemRepository.create(transactor)
      serverEndpoints = LotterySystemService.serverEndpoints(repository, apiKey)
      swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints(
        serverEndpoints,
        "Lottery System",
        "1.0"
      )

      routes = Http4sServerInterpreter[IO]().toRoutes(
        serverEndpoints ++ swaggerEndpoints
      )

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString("localhost").get)
        .withPort(port)
        .withHttpApp(Router("/" -> routes).orNotFound)
        .build
        .use { server =>
          for {
            _ <- IO.println(
              s"Go to http://localhost:${server.address.getPort}/docs to open SwaggerUI. Press ENTER key to exit."
            )
            _ <- IO.readLine
          } yield ()
        }
    } yield ExitCode.Success
  }

}
