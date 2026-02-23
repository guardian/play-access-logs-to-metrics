package com.gu.alb

import com.gu.alb.AthenaClient
import com.gu.alb.models.{AppIdentity, EndpointAggregate, LogAggregates}
import play.routes.compiler.{Include, Route, Rule}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait LogService:
  def calculateAggregates(
      appIdentity: AppIdentity,
      day: LocalDate,
      rules: Seq[Rule]
  ): LogAggregates

class LogServiceImpl(
    athenaClient: AthenaClient
) extends LogService:

  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

  def rulesToSqlCaseStatement(rules: Seq[Rule]): Seq[String] =
    rules
      .map {
        case Include(_, _) =>
          throw new UnsupportedOperationException("Include rules are not supported in SQL case statement")
        case route: Route =>
          val parts = route.path.parts.map {
            case play.routes.compiler.StaticPart(value)        => value
            case play.routes.compiler.DynamicPart(_, regex, _) => regex // once parsed the dynamic part is a jvm regex
          }
          // we wrap the path regexp into a full URL regexp to avoid matching greedily
          // ^https://[^/]+/ means the string needs to start with https, and we'll match anything that doesn't contain a slash
          // in other words: we'll match the domain name
          val regexp = s"^https://[^/]+/${parts.mkString}$$"
          s"""WHEN regexp_like(request_url, '$regexp') THEN '${route.verb} /${route.path}'"""
      }

  override def calculateAggregates(
      appIdentity: AppIdentity,
      day: LocalDate,
      rules: Seq[Rule]
  ): LogAggregates =
    val sql_case_statements = rulesToSqlCaseStatement(rules).mkString("\n    ")
    val query =
      s"""
         |SELECT CASE
         |    $sql_case_statements
         |    ELSE 'UNMATCHED'
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

    val endpointAggregates: Seq[(String, Long)] = athenaClient.executeQuery(query) { row =>
      (row.data().get(0).varCharValue(), row.data().get(1).varCharValue().toLong)
    }

    val endpoints = rules.map {
      case route: Route =>
        endpointAggregates
          .find(_._1 == s"${route.verb} /${route.path}") match {
          case Some((playEndpoint, requestCount)) => EndpointAggregate(playEndpoint, requestCount, route.call.toString)
          case None => EndpointAggregate(s"${route.verb} /${route.path}", 0, route.call.toString)
        }
      case _ => throw new UnsupportedOperationException("Only Route rules are supported when calculating aggregates")
    }

    LogAggregates(
      appIdentity = appIdentity,
      day = day,
      endpoints = endpoints
    )
