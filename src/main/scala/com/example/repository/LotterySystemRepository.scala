package com.example.repository

import cats.effect.Async
import com.example.domain._
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.{ query, update }

import java.time.LocalDate
import java.util.UUID

trait LotterySystemRepository[F[_]] {

  def createPrincipal(id: Principal): F[Int]

  def getPrincipal(id: PrincipalId): F[Principal]

  def createLottery(lottery: Lottery): F[Int]

  def closeLottery(id: LotteryId, winnerBallotId: BallotId): F[Int]

  def getLottery(id: LotteryId): F[Lottery]

  def getLotteries: F[List[Lottery]]

  def createBallot(ballot: Ballot): F[Int]

  def getBallotsForLottery(lotteryId: LotteryId): F[List[Ballot]]

}

object LotterySystemRepository {

  object Query {

    def createPrincipal(id: UUID): update.Update0 = sql"INSERT INTO principal VALUES ($id)".update

    def getPrincipal(id: UUID): query.Query0[Principal] = sql"SELECT * FROM principal WHERE id = $id".query[Principal]

    def createLottery(id: UUID, endDate: LocalDate): update.Update0 =
      sql"INSERT INTO lottery VALUES ($id, $endDate)".update

    def closeLottery(id: UUID, winnerBallotId: UUID): update.Update0 =
      sql"UPDATE lottery SET winner_ballot_id = $winnerBallotId WHERE id = $id".update

    def getLottery(id: UUID): query.Query0[Lottery] = sql"SELECT * FROM lottery WHERE id = $id".query[Lottery]

    def getLotteries: query.Query0[Lottery] =
      sql"SELECT * FROM lottery"
        .query[Lottery]

    def createBallot(
        ballotId: UUID,
        lotteryId: UUID,
        principalId: UUID
    ): update.Update0 = sql"INSERT INTO ballot VALUES ($ballotId, $lotteryId, $principalId)".update

    def getBallotForLottery(uuid: UUID): query.Query0[Ballot] =
      sql"SELECT * FROM ballot WHERE lottery_id = $uuid".query[Ballot]

  }

  def create[F[_]: Async](
      transactor: Transactor[F]
  ): LotterySystemRepository[F] =
    new LotterySystemRepository[F] {

      override def createPrincipal(principal: Principal): F[Int] =
        Query.createPrincipal(principal.id.value).run.transact(transactor)

      override def getPrincipal(principal: PrincipalId): F[Principal] =
        Query.getPrincipal(principal.value).unique.transact(transactor)

      override def createLottery(lottery: Lottery): F[Int] =
        Query
          .createLottery(lottery.lotteryId.value, lottery.endDate)
          .run
          .transact(transactor)

      override def closeLottery(
          lotteryId: LotteryId,
          winnerBallotId: BallotId
      ): F[Int] =
        Query
          .closeLottery(lotteryId.value, winnerBallotId.value)
          .run
          .transact(transactor)

      override def getLottery(lotteryId: LotteryId): F[Lottery] =
        Query.getLottery(lotteryId.value).unique.transact(transactor)

      override def getLotteries: F[List[Lottery]] =
        Query.getLotteries.stream.compile.toList
          .transact(transactor)

      override def createBallot(ballot: Ballot): F[Int] =
        Query
          .createBallot(
            ballot.ballotId.value,
            ballot.lotteryId.value,
            ballot.principalId.value
          )
          .run
          .transact(transactor)

      override def getBallotsForLottery(
          lotteryId: LotteryId
      ): F[List[Ballot]] =
        Query
          .getBallotForLottery(lotteryId.value)
          .stream
          .compile
          .toList
          .transact(transactor)

    }

}
