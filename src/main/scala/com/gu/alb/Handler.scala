package com.gu.alb

import com.gu.alb.models.AppIdentity
import org.slf4j.LoggerFactory
import play.routes.compiler.RoutesFileParser
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}

import java.time.LocalDate
import scala.jdk.CollectionConverters.*

class Handler:

  private val logger = LoggerFactory.getLogger(this.getClass)

  def handle(envVars: Map[String, String] = System.getenv().asScala.toMap): Unit =
    val config = Config.load(envVars)

    val credentials: AwsCredentialsProvider = DefaultCredentialsProvider
      .builder()
      .profileName("frontend")
      .build()
    val athenaClient = new AthenaClientImpl(credentials, "gucdk_access_logs", config.athenaOutputLocation)
    val logService = new LogServiceImpl(athenaClient)
    val routesFetcher = new RoutesFetcherImpl()
    val cloudwatchClient = new CloudwatchClientImpl(credentials)

    process(config, logService, routesFetcher, cloudwatchClient)

  def process(
      config: LambdaConfig,
      logService: LogService,
      routesFetcher: RoutesFetcher,
      cloudwatchClient: CloudwatchClient
  ): Unit =
    config.apps.foreach(appConfig =>
      val day = LocalDate.now().minusDays(1)
      logger.info(
        s"Processing app: ${appConfig.app}, stack: ${appConfig.stack}, stage: ${appConfig.stage}, for day: $day"
      )

      val routesFile = routesFetcher.fetchRoutes(appConfig.routesUrl)
      val rules = RoutesFileParser.parse(routesFile) match
        case Left(errors) =>
          logger.error(s"Failed to parse routes file: ${errors.map(_.message).mkString(", ")}")
          throw new RuntimeException("Failed to parse routes file")
        case Right(rules) =>
          logger.info(s"Successfully parsed routes file with ${rules.size} routes")
          rules

      val appIdentity = AppIdentity(app = appConfig.app, stack = appConfig.stack, stage = appConfig.stage)
      val results = logService.calculateAggregates(appIdentity, day, rules)

      cloudwatchClient.pushAggregateMetrics(results)

      logger.info(results.toString)
    )
