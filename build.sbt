import org.typelevel.sbt.tpolecat.*
import org.typelevel.scalacoptions.ScalacOptions

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.15"


val doobieVersion = "1.0.0-RC6"
val tapirVersion  = "1.11.7"

lazy val root = (project in file("."))
  .settings(
    name := "lottery-system",
    libraryDependencies ++= Seq(
      "org.http4s"                  %% "http4s-ember-server"     % "0.23.28",
      "ch.qos.logback"              % "logback-classic"          % "1.5.11",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion,
      "io.circe"                    %% "circe-generic-extras"    % "0.14.4",
      "org.tpolecat"                %% "doobie-core"             % doobieVersion,
      "org.tpolecat"                %% "doobie-postgres"         % doobieVersion,
      "org.postgresql"              % "postgresql"               % "42.7.4",
      // better monadic for compiler plugin as recommended
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      // Testing libraries
      "org.scalatest"                 %% "scalatest-flatspec"            % "3.2.19"      % Test,
      "org.mockito"                   %% "mockito-scala-scalatest"       % "1.17.37"     % Test,
      "org.tpolecat"                  %% "doobie-scalatest"              % doobieVersion % Test,
      "org.typelevel"                 %% "cats-effect-testing-scalatest" % "1.5.0"       % Test,
      "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server"        % tapirVersion  % Test,
      "com.softwaremill.sttp.client3" %% "circe"                         % "3.9.8"       % Test
    )
  )
