package com.gu.alb

import scala.jdk.CollectionConverters.*

object LocalRun:
  def main(args: Array[String]): Unit =
    val mockedEnvVars: Map[String, String] = Map(
      "ATHENA_OUTPUT_LOCATION" -> "s3://aws-frontend-logs/athena-output/",
      "PLAY_ACCESS_LOGS_CONFIG" ->
        """{
          |  "apps": [
          |    {
          |      "app": "archive",
          |      "stack": "frontend",
          |      "stage": "CODE",
          |      "routesUrl": "https://raw.githubusercontent.com/guardian/frontend/refs/heads/main/archive/conf/routes"
          |    },
          |    {
          |      "app": "article",
          |      "stack": "frontend",
          |      "stage": "CODE",
          |      "routesUrl": "https://raw.githubusercontent.com/guardian/frontend/refs/heads/main/article/conf/routes"
          |    },
          |    {
          |      "app": "commercial",
          |      "stack": "frontend",
          |      "stage": "CODE",
          |      "routesUrl": "https://raw.githubusercontent.com/guardian/frontend/refs/heads/main/commercial/conf/routes"
          |    },
          |    {
          |      "app": "facia",
          |      "stack": "frontend",
          |      "stage": "CODE",
          |      "routesUrl": "https://raw.githubusercontent.com/guardian/frontend/refs/heads/main/facia/conf/routes"
          |    },
          |    {
          |      "app": "onward",
          |      "stack": "frontend",
          |      "stage": "CODE",
          |      "routesUrl": "http://raw.githubusercontent.com/guardian/frontend/refs/heads/main/onward/conf/routes"
          |    },
          |    {
          |      "app": "rss",
          |      "stack": "frontend",
          |      "stage": "CODE",
          |      "routesUrl": "http://raw.githubusercontent.com/guardian/frontend/refs/heads/main/rss/conf/routes"
          |    },
          |    {
          |      "app": "sport",
          |      "stack": "frontend",
          |      "stage": "CODE",
          |      "routesUrl": "http://raw.githubusercontent.com/guardian/frontend/refs/heads/main/sport/conf/routes"
          |    }
          |  ]
          |}""".stripMargin
    )

    val handler = new Handler:
      override protected def envVars: Map[String, String] = mockedEnvVars

    handler.handleRequest(Map("day" -> "2026-02-05").asJava, null)
