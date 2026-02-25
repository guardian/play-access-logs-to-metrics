val scala3Version = "3.8.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "play-access-logs-to-metrics",
    version := "1.0.0",

    scalaVersion := scala3Version,

    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.4.0",
    libraryDependencies += "software.amazon.awssdk" % "athena" % "2.41.22",
    libraryDependencies += "software.amazon.awssdk" % "cloudwatch" % "2.41.22",
    libraryDependencies += "software.amazon.awssdk" % "ssm" % "2.41.22", // only used to run locally
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.17",
    libraryDependencies += "org.playframework" %% "play-routes-compiler" % "3.0.10",
    libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "5.3.2",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "4.4.2",
    libraryDependencies += "org.scalameta" %% "munit" % "1.2.2" % Test,
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
