package com.example.repository

import cats.effect.IO
import doobie._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate
import java.util.UUID

class LotterySystemRepositorySpec extends AnyFunSuite with IOChecker {

  private val user = sys.env("POSTGRES_USER")

  private val password = sys.env("POSTGRES_PASSWORD")

  override val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = s"jdbc:postgresql:$user",
    user = user,
    password = password,
    logHandler = None
  )

  private val uuid: UUID = UUID.randomUUID()

  private val date = LocalDate.now()

  test("createPrincipal") {
    check(LotterySystemRepository.Query.createPrincipal(uuid))
  }

  test("getPrincipal") {
    check(LotterySystemRepository.Query.getPrincipal(uuid))
  }

  test("createLottery") {
    check(LotterySystemRepository.Query.createLottery(uuid, date))
  }

  test("closeLottery") {
    check(LotterySystemRepository.Query.closeLottery(uuid, uuid))
  }

  test("getOpenLotteries")(check(LotterySystemRepository.Query.getLotteries))

  test("createBallot") {
    check(LotterySystemRepository.Query.createBallot(uuid, uuid, uuid))
  }

  test("getBallotForLottery") {
    check(LotterySystemRepository.Query.getBallotForLottery(uuid))
  }

}
