package com.gu.alb

import com.gu.alb.athena.AthenaClientImpl
import com.gu.alb.logs.LogServiceImpl
import com.gu.alb.models.AppIdentity
import org.slf4j.LoggerFactory

import java.time.LocalDate

class Handler:

  private val logger = LoggerFactory.getLogger(this.getClass)

  def handle(): Unit =
    val athenaClient = new AthenaClientImpl("gucdk_access_logs", "s3://my-bucket/athena-output/")
    val logService = new LogServiceImpl(athenaClient)

    val appIdentity = AppIdentity(app = "facia", stack = "frontend", stage = "PROD")
    val day = LocalDate.of(2026, 1, 29)
    val results = logService.calculateAggregates(appIdentity, day)
    logger.info(results.toString)
