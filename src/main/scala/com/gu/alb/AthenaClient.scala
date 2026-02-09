package com.gu.alb

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.athena.AthenaClient as AWSAthenaClient
import software.amazon.awssdk.services.athena.model.*

import scala.jdk.CollectionConverters.*

trait AthenaClient:
  def executeQuery[A](query: String)(parseRow: Row => A): Seq[A]

class AthenaClientImpl(
    credentials: AwsCredentialsProvider,
    database: String,
    outputLocation: String
) extends AthenaClient:

  private val athenaClient: AWSAthenaClient = AWSAthenaClient
    .builder()
    .region(Region.EU_WEST_1)
    .credentialsProvider(credentials)
    .build()

  override def executeQuery[A](query: String)(parseRow: Row => A): Seq[A] =
    // Start query execution
    val queryExecutionContext = QueryExecutionContext
      .builder()
      .database(database)
      .build()

    val resultConfiguration = ResultConfiguration
      .builder()
      .outputLocation(outputLocation)
      .build()

    val startQueryRequest = StartQueryExecutionRequest
      .builder()
      .queryString(query)
      .queryExecutionContext(queryExecutionContext)
      .resultConfiguration(resultConfiguration)
      .build()

    val startQueryResponse = athenaClient.startQueryExecution(startQueryRequest)
    val queryExecutionId = startQueryResponse.queryExecutionId()

    // Wait for query to complete
    waitForQueryCompletion(queryExecutionId)

    // Get results
    getQueryResults(queryExecutionId)(parseRow)

  private def waitForQueryCompletion(queryExecutionId: String): Unit =
    val getQueryExecutionRequest = GetQueryExecutionRequest
      .builder()
      .queryExecutionId(queryExecutionId)
      .build()

    var isRunning = true
    while isRunning do
      val response = athenaClient.getQueryExecution(getQueryExecutionRequest)
      val state = response.queryExecution().status().state()

      state match
        case QueryExecutionState.SUCCEEDED =>
          isRunning = false
        case QueryExecutionState.FAILED =>
          val reason = response.queryExecution().status().stateChangeReason()
          throw RuntimeException(s"Query failed: $reason")
        case QueryExecutionState.CANCELLED =>
          throw RuntimeException("Query was cancelled")
        case _ =>
          Thread.sleep(1000) // Poll every second

  private def getQueryResults[A](queryExecutionId: String)(parseRow: Row => A): Seq[A] =
    val getQueryResultsRequest = GetQueryResultsRequest
      .builder()
      .queryExecutionId(queryExecutionId)
      .build()

    val results = athenaClient.getQueryResults(getQueryResultsRequest)
    val rows = results.resultSet().rows().asScala.toSeq

    // Skip header row
    rows.drop(1).map(parseRow)
