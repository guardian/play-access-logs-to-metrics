package com.gu.alb

object LocalRun:
  def main(args: Array[String]): Unit = {
    val envVars: Map[String, String] = Map(
      "ATHENA_OUTPUT_LOCATION" -> "s3://not-a-bucket/athena-output/",
      "PLAY_ACCESS_LOGS_CONFIG" ->
        """{
        |  "apps": [
        |    {
        |      "app": "facia",
        |      "stack": "frontend",
        |      "stage": "PROD",
        |      "routesUrl": "https://raw.githubusercontent.com/guardian/frontend/refs/heads/main/facia/conf/routes"
        |    },
        |    {
        |      "app": "onward",
        |      "stack": "frontend",
        |      "stage": "PROD",
        |      "routesUrl": "http://raw.githubusercontent.com/guardian/frontend/refs/heads/main/onward/conf/routes"
        |    }
        |  ]
        |}""".stripMargin
    )

    new Handler().handle(envVars)
  }
