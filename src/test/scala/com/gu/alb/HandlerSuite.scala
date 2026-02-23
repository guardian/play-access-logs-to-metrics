package com.gu.alb

import com.gu.alb.models.{AppIdentity, LogAggregates}
import org.scalamock.stubs.Stubs
import play.routes.compiler.*

import java.io.File
import java.time.LocalDate

class HandlerSuite extends munit.FunSuite with Stubs:

  private val day = LocalDate.of(2026, 1, 19)

  // Helper to create a static route (e.g., GET /_healthcheck)
  private def staticRoute(verb: String, path: String): Route =
    Route(
      verb = HttpVerb(verb),
      path = PathPattern(Seq(StaticPart(path))),
      call = HandlerCall(Some("controllers"), "TestController", instantiate = false, "test", None),
      comments = Nil
    )

  // Mock RoutesFetcher
  private class MockRoutesFetcher(content: String) extends RoutesFetcherImpl:
    override def fetchRoutes(url: String): File =
      val tempFile = File.createTempFile("routes", ".routes")
      tempFile.deleteOnExit()
      val writer = new java.io.FileWriter(tempFile)
      writer.write(content)
      writer.close()
      tempFile

  test("process - successful processing calls calculateAggregates with correct parameters") {
    val routesContent = """GET /_healthcheck controllers.TestController.healthcheck()"""
    val mockRoutesFetcher = MockRoutesFetcher(routesContent)
    val mockLogService = stub[LogService]
    mockLogService.calculateAggregates returns { case (appId, day, rules) =>
      LogAggregates(appId, day, Seq.empty)
    }
    val cloudwatchClient = stub[CloudwatchClient]
    cloudwatchClient.pushAggregateMetrics.returns(_ => ())

    val config = LambdaConfig(
      apps = List(AppsConfig("facia", "frontend", "PROD", "https://example.com/routes")),
      athenaOutputLocation = "not-a-bucket"
    )

    val handler = Handler()
    handler.process(day, config, mockLogService, mockRoutesFetcher, cloudwatchClient)

    assertEquals(mockLogService.calculateAggregates.times, 1)
    assertEquals(mockLogService.calculateAggregates.calls.head._1, AppIdentity("facia", "frontend", "PROD"))
    assertEquals(mockLogService.calculateAggregates.calls.head._2, day)
    assertEquals(mockLogService.calculateAggregates.calls.head._3.length, 1)
  }

  test("process - processes multiple apps from config") {
    val routesContent = """GET /_healthcheck controllers.TestController.healthcheck()"""
    val mockRoutesFetcher = MockRoutesFetcher(routesContent)

    val mockLogService = stub[LogService]
    mockLogService.calculateAggregates returns { case (appId, day, rules) =>
      LogAggregates(appId, day, Seq.empty)
    }

    val cloudwatchClient = stub[CloudwatchClient]
    cloudwatchClient.pushAggregateMetrics.returns(_ => ())

    val config = LambdaConfig(
      apps = List(
        AppsConfig("facia", "frontend", "PROD", "https://example.com/routes1"),
        AppsConfig("mobile", "backend", "CODE", "https://example.com/routes2"),
        AppsConfig("article", "frontend", "PROD", "https://example.com/routes3")
      ),
      athenaOutputLocation = "still-not-a-bucket"
    )

    val handler = Handler()
    handler.process(day, config, mockLogService, mockRoutesFetcher, cloudwatchClient)

    assertEquals(mockLogService.calculateAggregates.times, 3)
  }
