val scala3Version = "3.8.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "play-access-logs-to-metrics",
    version := "1.0.0",

    scalaVersion := scala3Version,

    libraryDependencies += "software.amazon.awssdk" % "athena" % "2.41.22",
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.17",
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
  )
