ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

Compile / run / fork := true

ThisBuild / Compile / run / fork := true

val weaverVersion     = "0.8.1"
val http4sVersion     = "0.23.11"
val fs2Version        = "3.11.0"
val log4catsVersion   = "2.3.1"
val CirceVersion      = "0.14.2"
val CirisConfig       = "2.3.2"
val ScalatestVersion  = "3.2.15"
val refinedVersion    = "0.11.0"
val enumeratumVersion = "1.7.0"

lazy val root = (project in file("."))
  .settings(
    name := "clearscore-tt",
    libraryDependencies ++= Seq(
      "org.typelevel"       %% "cats-effect"          % "3.5.4",
      "com.disneystreaming" %% "weaver-scalacheck"    % weaverVersion % Test,
      "com.disneystreaming" %% "weaver-cats"          % weaverVersion % Test,
      "org.http4s"          %% "http4s-ember-client"  % http4sVersion,
      "org.http4s"          %% "http4s-ember-server"  % http4sVersion,
      "org.http4s"          %% "http4s-dsl"           % http4sVersion,
      "org.typelevel"       %% "log4cats-slf4j"       % "2.6.0",
      "ch.qos.logback"       % "logback-classic"      % "1.2.11",
      "org.http4s"          %% "http4s-dsl"           % http4sVersion,
      "io.circe"            %% "circe-generic"        % CirceVersion,
      "io.circe"            %% "circe-generic-extras" % CirceVersion,
      "io.circe"            %% "circe-parser"         % CirceVersion,
      "io.circe"            %% "circe-refined"        % CirceVersion,
      "org.http4s"          %% "http4s-circe"         % http4sVersion,
      "is.cir"              %% "ciris"                % CirisConfig,
      "org.scalatest" %% "scalatest"        % ScalatestVersion % "test,it",
      "eu.timepit"    %% "refined"          % refinedVersion,
      "eu.timepit"    %% "refined-cats"     % refinedVersion,
      "com.beachape"  %% "enumeratum"       % enumeratumVersion,
      "com.beachape"  %% "enumeratum-circe" % enumeratumVersion
//      "eu.timepit"    %% "refined-circe" % refinedVersion
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions += "-Wnonunit-statement"
  )
