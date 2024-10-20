package com.example.api

import com.example.domain._
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

import java.time.LocalDate
import java.util.UUID

object Endpoint {

  private def lotteries = "lotteries"

  def newPrincipalEndpoint: Endpoint[Unit, Unit, Unit, Principal, Any] =
    endpoint.post
      .in("principals")
      .out(jsonBody[Principal].and(statusCode(StatusCode.Created)))

  def putBallotEndpoint: Endpoint[PrincipalId, LotteryId, Unit, StatusCode, Any] =
    endpoint.post
      .in(lotteries / path[LotteryId]("lotteryId") / "ballot")
      .securityIn(auth.bearer[PrincipalId]())
      .out(
        statusCode
          .description(StatusCode.Created, "Ballot Created")
          .description(StatusCode.Forbidden, "Lottery Closed")
      )
      .errorOut(statusCode(StatusCode.Unauthorized))

  def getLotteriesEndpoint: Endpoint[Unit, (Option[Status], Option[LocalDate]), Unit, List[
    Lottery
  ], Any] =
    endpoint.get
      .in(lotteries)
      .in(query[Option[Status]]("status"))
      .in(query[Option[LocalDate]]("endDate"))
      .out(jsonBody[List[Lottery]])

  private def adminGenericLotteriesEndpoint = endpoint.in("admin" / lotteries).securityIn(auth.bearer[UUID]())

  def adminCreateLotteryEndpoint: Endpoint[UUID, LocalDate, Unit, Unit, Any] =
    adminGenericLotteriesEndpoint.post
      .in(jsonBody[LocalDate])
      .out(statusCode(StatusCode.Created))
      .errorOut(statusCode(StatusCode.Unauthorized))

  def adminCloseLotteryEndpoint: Endpoint[UUID, LotteryId, Unit, StatusCode, Any] =
    adminGenericLotteriesEndpoint.put
      .in(path[LotteryId]("lotteryId") / "close")
      .out(
        statusCode
          .description(StatusCode.Ok, "Lottery Closed")
          .description(StatusCode.Forbidden, "Lottery already closed or there are no ballots for the lottery")
      )
      .errorOut(statusCode(StatusCode.Unauthorized))

}
