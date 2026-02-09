val scala3Version = "3.8.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "play-access-logs-to-metrics",
    version := "1.0.0",

    scalaVersion := scala3Version,

    libraryDependencies += "software.amazon.awssdk" % "athena" % "2.41.22",
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.17",
    libraryDependencies += "com.typesafe.play" %% "routes-compiler" % "2.9.0-M6",
    libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "4.12.0",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "4.4.2",
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
    libraryDependencies += "org.scalamock" %% "scalamock" % "7.5.5" % Test,

    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "okio.kotlin_module")                 => MergeStrategy.first
      case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties")       => MergeStrategy.concat
      case x                                                          =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
