package com.gu.alb

import okhttp3.{OkHttpClient, Request}
import org.slf4j.LoggerFactory

import java.io.File

trait RoutesFetcher:
  def fetchRoutes(url: String): File

class RoutesFetcherImpl extends RoutesFetcher:
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val okhttp = new OkHttpClient()
  override def fetchRoutes(url: String): File =
    val request = new Request.Builder()
      .url(url)
      .build
    val response = okhttp.newCall(request).execute()

    if response.isSuccessful then
      val tempFile = File.createTempFile("routes", "")
      logger.debug("Created temporary file for routes: {}", tempFile.getAbsolutePath)
      tempFile.deleteOnExit()
      val body = response.body()
      if body != null then {
        val source = body.source()
        val sink = okio.Okio.sink(tempFile)
        source.readAll(sink)
        sink.close()
        tempFile
      } else {
        throw new RuntimeException(s"Failed to fetch routes: empty response body from $url")
      }
    else throw new RuntimeException(s"Failed to fetch routes: ${response.code()} ${response.message()} from $url")
