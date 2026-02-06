package com.gu.alb.logs

import com.gu.alb.athena.AthenaClient
import com.gu.alb.models.{AppIdentity, EndpointAggregate, LogAggregates}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait LogService:
  def calculateAggregates(
      appIdentity: AppIdentity,
      day: LocalDate
  ): LogAggregates

class LogServiceImpl(
    athenaClient: AthenaClient
) extends LogService:

  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

  override def calculateAggregates(
      appIdentity: AppIdentity,
      day: LocalDate
  ): LogAggregates =
    // TODO: Replace with real query
    val query =
      s"""
         |SELECT CASE
         |    WHEN request_url LIKE '/assets/%' THEN 'GET /assets/*path'
         |    WHEN request_url = '/_healthcheck' THEN 'GET /_healthcheck'
         |    WHEN request_url = '/_fronts_cdn_healthcheck' THEN 'GET /_fronts_cdn_healthcheck'
         |    WHEN request_url = '/_agentcontents' THEN 'GET /_agentcontents'
         |    WHEN request_url LIKE '/container/count/%/offset/%/section/%/mf2.json' THEN 'GET /container/count/:count/offset/:offset/section/:section/mf2.json'
         |    WHEN request_url LIKE '/container/count/%/offset/%/mf2.json' THEN 'GET /container/count/:count/offset/:offset/mf2.json'
         |    WHEN request_url LIKE '/collection/%/rss' THEN 'GET /collection/*id/rss'
         |    WHEN request_url LIKE '/container/use-layout/%.json' THEN 'GET /container/use-layout/*id.json'
         |    WHEN request_url LIKE '/container/data/%.json' THEN 'GET /container/data/*id.json'
         |    WHEN request_url LIKE '/container/%.json' THEN 'GET /container/*id.json'
         |    WHEN request_url LIKE '%/show-more/%.json' THEN 'GET /*path/show-more/*id.json'
         |    WHEN request_url LIKE '%/rss' THEN 'GET /*path/rss'
         |    WHEN request_url LIKE '%/lite.json' THEN 'GET /*path/lite.json'
         |    WHEN request_url LIKE '%.emailjson' THEN 'GET /*path.emailjson'
         |    WHEN request_url LIKE '%.emailtxt' THEN 'GET /*path.emailtxt'
         |    WHEN request_url LIKE '%.json' THEN 'GET /*path.json'
         |    WHEN request_url LIKE '%/headline.txt' THEN 'GET /*path/headline.txt'
         |    WHEN request_url = '/' THEN 'GET /'
         |    ELSE 'GET /*path (catch-all)'
         |  END AS route_pattern,
         |  COUNT(*) AS request_count
         |FROM "gucdk_access_logs"."alb_access_logs"
         |WHERE app = '${appIdentity.app}'
         |  AND stack = '${appIdentity.stack}'
         |  AND stage = '${appIdentity.stage}'
         |  AND day = '${dateFormatter.format(day)}'
         |GROUP BY 1
         |ORDER BY request_count DESC
         |""".stripMargin

    val endpointAggregates: Seq[EndpointAggregate] = athenaClient.executeQuery(query) { row =>
      EndpointAggregate(
        playEndpoint = row.data().get(0).varCharValue(),
        requestCount = row.data().get(1).varCharValue().toLong
      )
    }

    LogAggregates(
      appIdentity = appIdentity,
      day = day,
      endpoints = endpointAggregates
    )
