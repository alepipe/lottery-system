package com.example.domain

import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveUnwrappedEncoder
import sttp.tapir.Codec
import sttp.tapir.Codec.PlainCodec

import java.time.LocalDate
import java.util.UUID

final case class LotteryId(value: UUID) extends AnyVal

object LotteryId {

  implicit val lotteryIdEncoder: Encoder[LotteryId] = deriveUnwrappedEncoder

}

final case class Lottery(
    lotteryId: LotteryId,
    endDate: LocalDate,
    winnerBallotId: Option[BallotId]
)

sealed trait Status

object Status {

  case object Open   extends Status
  case object Closed extends Status

  implicit val statusCodec: PlainCodec[Status] =
    Codec.derivedEnumeration[String, Status].defaultStringBased

}
