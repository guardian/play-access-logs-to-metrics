package com.gu.alb

import com.gu.alb.athena.AthenaClientImpl
import com.gu.alb.logs.LogServiceImpl
import com.gu.alb.models.AppIdentity
import org.slf4j.LoggerFactory
import play.routes.compiler.RoutesFileParser

import java.time.LocalDate

class Handler:

  private val logger = LoggerFactory.getLogger(this.getClass)

  def handle(): Unit =
    val athenaClient = new AthenaClientImpl("gucdk_access_logs", "s3://my-bucket/athena-output/")
    val logService = new LogServiceImpl(athenaClient)
    val routesFetcher = new RoutesFetcherImpl()

    val faciaRoutesFile = "https://raw.githubusercontent.com/guardian/frontend/refs/heads/main/facia/conf/routes"
    val routesFile = routesFetcher.fetchRoutes(faciaRoutesFile)
    logger.info(s"Fetched routes file to ${routesFile.getAbsolutePath}")

    val rules = RoutesFileParser.parse(routesFile) match
      case Left(errors) =>
        logger.error(s"Failed to parse routes file: ${errors.map(_.message).mkString(", ")}")
        throw new RuntimeException("Failed to parse routes file")
      case Right(rules) =>
        logger.info(s"Successfully parsed routes file with ${rules.size} routes")
        rules

    val appIdentity = AppIdentity(app = "facia", stack = "frontend", stage = "PROD")
    val day = LocalDate.of(2026, 1, 29)
    val results = logService.calculateAggregates(appIdentity, day, rules)
    logger.info(results.toString)
