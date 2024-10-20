package com.example.services

import cats.effect.Async
import cats.effect.std.Random
import cats.effect.std.UUIDGen
import cats.syntax.all._
import com.example.api
import com.example.domain._
import com.example.repository.LotterySystemRepository
import sttp.model.StatusCode

import java.time.LocalDate
import java.util.UUID

object LotterySystemService {

  def serverEndpoints[F[_]: Async](
      lotterySystemRepository: LotterySystemRepository[F],
      apiKey: UUID
  ) = {
    val lotterySystemService: LotterySystemService[F] =
      new LotterySystemService[F](lotterySystemRepository, apiKey)

    List(
      api.Endpoint.newPrincipalEndpoint.serverLogicSuccess(_ => lotterySystemService.newPrincipalServiceLogic),
      api.Endpoint.putBallotEndpoint
        .serverSecurityLogic(uuid => lotterySystemService.checkUserCredential(uuid))
        .serverLogicSuccess(p => lid => lotterySystemService.putBallotServiceLogic(p.id, lid)),
      api.Endpoint.getLotteriesEndpoint.serverLogicSuccess { case (maybeSatus, maybeDate) =>
        lotterySystemService.getLotteriesServiceLogic(maybeSatus, maybeDate)
      },
      api.Endpoint.adminCreateLotteryEndpoint
        .serverSecurityLogic(uuid => lotterySystemService.checkAdminCredential(uuid))
        .serverLogicSuccess(_ => endDate => lotterySystemService.adminCreateLotteryServiceLogic(endDate)),
      api.Endpoint.adminCloseLotteryEndpoint
        .serverSecurityLogic(uuid => lotterySystemService.checkAdminCredential(uuid))
        .serverLogicSuccess(_ => lotteryId => lotterySystemService.adminCloseLotteryServiceLogic(lotteryId))
    )

  }

}

private class LotterySystemService[F[_]: Async](
    lotterySystemRepository: LotterySystemRepository[F],
    apiKey: UUID
) {

  private def isLotteryClosed(lotteryId: LotteryId) =
    for {
      lottery <- lotterySystemRepository.getLottery(lotteryId)
      _ <- Async[F].fromEither(
        Either.cond(
          lottery.winnerBallotId.isEmpty,
          (),
          new Error("Lottery is already closed!")
        )
      )
    } yield ()

  def checkAdminCredential(uuid: UUID): F[Either[Unit, Unit]] = Async[F].pure(Either.cond(apiKey == uuid, (), ()))

  def checkUserCredential(
      principalId: PrincipalId
  ): F[Either[Unit, Principal]] =
    (for {
      principal <- lotterySystemRepository.getPrincipal(principalId)
    } yield principal.asRight[Unit]).recover { case _: Throwable =>
      ().asLeft[Principal]
    }

  def newPrincipalServiceLogic: F[Principal] =
    for {
      uuid <- UUIDGen.randomUUID[F]
      principal = Principal(PrincipalId(uuid))
      _ <- lotterySystemRepository.createPrincipal(principal)
    } yield principal

  def putBallotServiceLogic(
      principalId: PrincipalId,
      lotteryId: LotteryId
  ): F[StatusCode] =
    (for {
      _    <- isLotteryClosed(lotteryId)
      uuid <- UUIDGen.randomUUID[F]
      ballot = Ballot(BallotId(uuid), lotteryId, principalId)
      _ <- lotterySystemRepository.createBallot(ballot)
    } yield StatusCode.Created).recover { case _: Throwable =>
      StatusCode.Forbidden
    }

  def getLotteriesServiceLogic(
      maybeStatus: Option[Status],
      maybeEndDate: Option[LocalDate]
  ): F[List[Lottery]] = {

    val dateFilter: Lottery => Boolean = maybeEndDate.fold { _: Lottery => true } { endDate => (lottery: Lottery) =>
      lottery.endDate.isEqual(endDate)
    }

    val statusFiler: Lottery => Boolean = maybeStatus.fold { _: Lottery => true } {
      case Status.Open   => lottery: Lottery => lottery.winnerBallotId.isEmpty
      case Status.Closed => lottery: Lottery => lottery.winnerBallotId.isDefined
    }

    for {
      lotteries <- lotterySystemRepository.getLotteries
    } yield lotteries.filter(dateFilter).filter(statusFiler)
  }

  def adminCreateLotteryServiceLogic(endDate: LocalDate): F[Unit] =
    for {
      uuid <- UUIDGen.randomUUID[F]
      lottery = Lottery(LotteryId(uuid), endDate, None)
      _ <- lotterySystemRepository.createLottery(lottery)
    } yield ()

  def adminCloseLotteryServiceLogic(lotteryId: LotteryId): F[StatusCode] =
    (for {
      _              <- isLotteryClosed(lotteryId)
      lotteryBallots <- lotterySystemRepository.getBallotsForLottery(lotteryId)
      rnd            <- Random.scalaUtilRandom[F]
      winner         <- rnd.betweenInt(0, lotteryBallots.size).map(lotteryBallots)
      _              <- lotterySystemRepository.closeLottery(lotteryId, winner.ballotId)
    } yield StatusCode.Ok).recover { case _: Throwable =>
      StatusCode.Forbidden
    }

}
