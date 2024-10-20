package com.example.testUtils

import com.example.domain._
import java.time.LocalDate
import java.util.UUID

object TestData {

  val apiKey: UUID = UUID.randomUUID()

  val principalId: PrincipalId = PrincipalId(UUID.randomUUID())

  val principal: Principal = Principal(principalId)

  val ballotId: BallotId = BallotId(UUID.randomUUID())

  val lotteryId: LotteryId = LotteryId(UUID.randomUUID())

  val closedLotteryId: LotteryId = LotteryId(UUID.randomUUID())

  val date: LocalDate = LocalDate.now()

  val ballot: Ballot = Ballot(ballotId, lotteryId, principalId)

  val lottery: Lottery = Lottery(lotteryId, date, None)

  val closedLottery: Lottery = Lottery(closedLotteryId, date, Some(ballotId))

}
