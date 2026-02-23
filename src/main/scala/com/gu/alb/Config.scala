package com.gu.alb

import upickle.{ReadWriter, macroRW}

case class AppsConfig(
    app: String,
    stack: String,
    stage: String,
    routesUrl: String
)

object AppsConfig:
  implicit val appsConfigRW: ReadWriter[AppsConfig] = macroRW

case class InputJsonConfig(
    apps: List[AppsConfig]
)

object InputJsonConfig:
  implicit val inputJsonConfigRW: ReadWriter[InputJsonConfig] = macroRW

case class LambdaConfig(
    apps: List[AppsConfig],
    athenaOutputLocation: String
)

object Config:
  def load(envVars: Map[String, String]): LambdaConfig =
    val rawConfig = envVars.getOrElse(
      "PLAY_ACCESS_LOGS_CONFIG",
      throw new RuntimeException("Missing environment variable: PLAY_ACCESS_LOGS_CONFIG")
    )
    val athenaOutputLocation = envVars.getOrElse(
      "ATHENA_OUTPUT_LOCATION",
      throw new RuntimeException("Missing environment variable: ATHENA_OUTPUT_LOCATION")
    )

    val inputConfig = upickle.default.read[InputJsonConfig](rawConfig)
    LambdaConfig(
      apps = inputConfig.apps,
      athenaOutputLocation = athenaOutputLocation
    )
