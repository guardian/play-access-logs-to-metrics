package com.gu.alb

import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest

import scala.jdk.CollectionConverters.*

object LocalRun:
  private def getSsmParameter(ssmClient: SsmClient, parameterName: String): String =
    val request = GetParameterRequest
      .builder()
      .name(parameterName)
      .build()
    val response = ssmClient.getParameter(request)
    response.parameter().value()

  def main(args: Array[String]): Unit =
    val credentials: AwsCredentialsProvider = DefaultCredentialsProvider
      .builder()
      .profileName("frontend")
      .build()

    val ssmClient = SsmClient.builder().credentialsProvider(credentials).build()
    val stage = "CODE"
    val stack = "frontend"
    val appName = "play-access-logs-to-metrics"

    val playAccessLogsConfig = getSsmParameter(ssmClient, s"/$stage/$stack/$appName/inputConfig")
    val athenaOutputBucket = getSsmParameter(ssmClient, s"/$stage/$stack/$appName/athenaOutputBucket")

    val mockedEnvVars: Map[String, String] = Map(
      "ATHENA_OUTPUT_LOCATION" -> s"s3://$athenaOutputBucket/athena-output/",
      "PLAY_ACCESS_LOGS_CONFIG" -> playAccessLogsConfig
    )

    val handler = new Handler:
      override protected def envVars: Map[String, String] = mockedEnvVars

    handler.handleRequest(Map.empty.asJava, null)
