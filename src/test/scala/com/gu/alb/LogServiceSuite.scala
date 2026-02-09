package com.gu.alb

import com.gu.alb.models.{AppIdentity, EndpointAggregate}
import com.gu.alb.{AthenaClient, LogServiceImpl}
import play.routes.compiler.*
import software.amazon.awssdk.services.athena.model.{Datum, Row}

import java.time.LocalDate

class LogServiceSuite extends munit.FunSuite:

  // Helper to create a static route (e.g., GET /_healthcheck)
  private def staticRoute(verb: String, path: String): Route =
    Route(
      verb = HttpVerb(verb),
      path = PathPattern(Seq(StaticPart(path))),
      call = HandlerCall(Some("controllers"), "TestController", instantiate = false, "test", None),
      comments = Nil
    )

  // Helper to create a dynamic route (e.g., GET /assets/*path)
  private def dynamicRoute(verb: String, parts: Seq[PathPart]): Route =
    Route(
      verb = HttpVerb(verb),
      path = PathPattern(parts),
      call = HandlerCall(Some("controllers"), "TestController", instantiate = false, "test", None),
      comments = Nil
    )

  // Helper to create a mock AthenaClient that returns specified route counts
  private def mockAthenaClient(results: (String, Long)*): AthenaClient =
    new AthenaClient:
      override def executeQuery[A](query: String)(parseRow: Row => A): Seq[A] =
        results
          .map { (route, count) =>
            Row
              .builder()
              .data(
                Datum.builder().varCharValue(route).build(),
                Datum.builder().varCharValue(count.toString).build()
              )
              .build()
          }
          .map(parseRow)

  test("rulesToSqlCaseStatement - static GET route") {
    val logService = LogServiceImpl(null) // AthenaClient not needed for this test
    val route = staticRoute("GET", "_healthcheck")

    val result = logService.rulesToSqlCaseStatement(Seq(route))

    assertEquals(result.length, 1)
    assertEquals(
      result.head,
      "WHEN regexp_like(request_url, '^https://[^/]+/_healthcheck$') THEN 'GET /_healthcheck'"
    )
  }

  test("rulesToSqlCaseStatement - static POST route") {
    val logService = LogServiceImpl(null)
    val route = staticRoute("POST", "submit")

    val result = logService.rulesToSqlCaseStatement(Seq(route))

    assertEquals(result.length, 1)
    assertEquals(
      result.head,
      "WHEN regexp_like(request_url, '^https://[^/]+/submit$') THEN 'POST /submit'"
    )
  }

  test("rulesToSqlCaseStatement - dynamic route with .+ regex") {
    val logService = LogServiceImpl(null)
    val route = dynamicRoute(
      "GET",
      Seq(
        StaticPart("assets/"),
        DynamicPart("path", ".+", encode = false)
      )
    )

    val result = logService.rulesToSqlCaseStatement(Seq(route))

    assertEquals(result.length, 1)
    assertEquals(
      result.head,
      "WHEN regexp_like(request_url, '^https://[^/]+/assets/.+$') THEN 'GET /assets/$path<.+>'"
    )
  }

  test("rulesToSqlCaseStatement - dynamic route with [^/]+ regex") {
    val logService = LogServiceImpl(null)
    val route = dynamicRoute(
      "GET",
      Seq(
        StaticPart("container/count/"),
        DynamicPart("count", "[^/]+", encode = false),
        StaticPart("/offset/"),
        DynamicPart("offset", "[^/]+", encode = false),
        StaticPart("/mf2.json")
      )
    )

    val result = logService.rulesToSqlCaseStatement(Seq(route))

    assertEquals(result.length, 1)
    assertEquals(
      result.head,
      "WHEN regexp_like(request_url, '^https://[^/]+/container/count/[^/]+/offset/[^/]+/mf2.json$') THEN 'GET /container/count/$count<[^/]+>/offset/$offset<[^/]+>/mf2.json'"
    )
  }

  test("rulesToSqlCaseStatement - multiple routes") {
    val logService = LogServiceImpl(null)
    val routes = Seq(
      staticRoute("GET", "_healthcheck"),
      staticRoute("GET", "humans.txt"),
      staticRoute("POST", "submit")
    )

    val result = logService.rulesToSqlCaseStatement(routes)

    assertEquals(result.length, 3)
    assertEquals(
      result(0),
      "WHEN regexp_like(request_url, '^https://[^/]+/_healthcheck$') THEN 'GET /_healthcheck'"
    )
    assertEquals(
      result(1),
      "WHEN regexp_like(request_url, '^https://[^/]+/humans.txt$') THEN 'GET /humans.txt'"
    )
    assertEquals(
      result(2),
      "WHEN regexp_like(request_url, '^https://[^/]+/submit$') THEN 'POST /submit'"
    )
  }

  test("rulesToSqlCaseStatement - empty rules") {
    val logService = LogServiceImpl(null)
    val result = logService.rulesToSqlCaseStatement(Seq.empty)
    assertEquals(result, Seq.empty)
  }

  test("rulesToSqlCaseStatement - Include rule throws UnsupportedOperationException") {
    val logService = LogServiceImpl(null)
    val includeRule = Include("prefix", "router")

    intercept[UnsupportedOperationException] {
      logService.rulesToSqlCaseStatement(Seq(includeRule))
    }
  }

  test("calculateAggregates - matching routes return correct counts") {
    val logService = LogServiceImpl(
      mockAthenaClient(
        "GET /_healthcheck" -> 100L,
        "POST /submit" -> 50L
      )
    )
    val appIdentity = AppIdentity("facia", "frontend", "PROD")
    val day = LocalDate.of(2026, 1, 29)
    val rules = Seq(
      staticRoute("GET", "_healthcheck"),
      staticRoute("POST", "submit")
    )

    val result = logService.calculateAggregates(appIdentity, day, rules)

    assertEquals(result.appIdentity, appIdentity)
    assertEquals(result.day, day)
    assertEquals(result.endpoints.length, 2)
    assertEquals(result.endpoints(0), EndpointAggregate("GET /_healthcheck", 100, "controllers.TestController.test"))
    assertEquals(result.endpoints(1), EndpointAggregate("POST /submit", 50, "controllers.TestController.test"))
  }

  test("calculateAggregates - unmatched routes return count of 0") {
    val logService = LogServiceImpl(mockAthenaClient("GET /_healthcheck" -> 100L))
    val appIdentity = AppIdentity("facia", "frontend", "PROD")
    val day = LocalDate.of(2026, 1, 29)
    val rules = Seq(
      staticRoute("GET", "_healthcheck"),
      staticRoute("POST", "submit") // This route has no matching data
    )

    val result = logService.calculateAggregates(appIdentity, day, rules)

    assertEquals(result.endpoints.length, 2)
    assertEquals(result.endpoints(0), EndpointAggregate("GET /_healthcheck", 100, "controllers.TestController.test"))
    assertEquals(
      result.endpoints(1),
      EndpointAggregate("POST /submit", 0, "controllers.TestController.test")
    ) // Default to 0
  }

  test("calculateAggregates - generates correct SQL query") {
    var capturedQuery: String = ""
    val mockAthenaClient = new AthenaClient:
      override def executeQuery[A](query: String)(parseRow: Row => A): Seq[A] =
        capturedQuery = query
        Seq.empty

    val logService = LogServiceImpl(mockAthenaClient)
    val appIdentity = AppIdentity("facia", "frontend", "PROD")
    val day = LocalDate.of(2026, 1, 29)
    val rules = Seq(staticRoute("GET", "_healthcheck"))

    logService.calculateAggregates(appIdentity, day, rules)

    assert(capturedQuery.contains("app = 'facia'"))
    assert(capturedQuery.contains("stack = 'frontend'"))
    assert(capturedQuery.contains("stage = 'PROD'"))
    assert(capturedQuery.contains("day = '2026/01/29'"))
    assert(
      capturedQuery.contains("WHEN regexp_like(request_url, '^https://[^/]+/_healthcheck$') THEN 'GET /_healthcheck'")
    )
    assert(capturedQuery.contains("ELSE 'UNMATCHED'"))
  }
