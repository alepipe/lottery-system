package com.example.domain

import io.circe.Encoder
import io.circe.generic.extras.semiauto._

import java.util.UUID

final case class BallotId(value: UUID) extends AnyVal

object BallotId {

  implicit val ballotIdEncoder: Encoder[BallotId] = deriveUnwrappedEncoder

}

final case class Ballot(
    ballotId: BallotId,
    lotteryId: LotteryId,
    principalId: PrincipalId
)
