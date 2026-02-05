val scala3Version = "3.8.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "play-access-logs-to-metrics",
    version := "1.0.0",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
  )
