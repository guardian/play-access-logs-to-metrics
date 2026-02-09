package com.gu.alb.models

import com.gu.alb.models.AppIdentity

import java.time.LocalDate

case class EndpointAggregate(
    playEndpoint: String,
    requestCount: Long,
    controllerMethod: String
)

case class LogAggregates(
    appIdentity: AppIdentity,
    day: LocalDate,
    endpoints: Seq[EndpointAggregate]
)
