package com.gu.alb

import com.gu.alb.models.LogAggregates
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest, StandardUnit}

import java.time.ZoneId
import scala.jdk.CollectionConverters.*

trait CloudwatchClient:
  def pushAggregateMetrics(aggregates: LogAggregates): Unit

class CloudwatchClientImpl(
    credentials: AwsCredentialsProvider
) extends CloudwatchClient:

  private val cloudwatchClient = CloudWatchClient
    .builder()
    .credentialsProvider(credentials)
    .build()

  private def dimension(key: String, value: String): Dimension =
    Dimension
      .builder()
      .name(key)
      .value(value)
      .build()

  override def pushAggregateMetrics(aggregates: LogAggregates): Unit = {
    // start of next day = end of current day
    val metricTimestamp = aggregates.day.plusDays(1).atStartOfDay(ZoneId.of("Europe/London")).toInstant
    val metricDatum = aggregates.endpoints.map { endpointAggregate =>
      // Convert EndpointAggregate to MetricDatum
      MetricDatum
        .builder()
        .metricName("RequestCount")
        .value(endpointAggregate.requestCount.toDouble)
        .dimensions(
          dimension("PlayEndpoint", endpointAggregate.playEndpoint),
          dimension("PlayControllerMethod", endpointAggregate.controllerMethod),
          dimension("App", aggregates.appIdentity.app),
          dimension("Stack", aggregates.appIdentity.stack),
          dimension("Stage", aggregates.appIdentity.stage)
        )
        .timestamp(metricTimestamp)
        .unit(StandardUnit.COUNT)
        .build()
    }
    val putDataRequest: PutMetricDataRequest = PutMetricDataRequest
      .builder()
      .metricData(metricDatum.asJava)
      .namespace("ALB/PlayRequestCount")
      .build()
    cloudwatchClient.putMetricData(putDataRequest)
  }
