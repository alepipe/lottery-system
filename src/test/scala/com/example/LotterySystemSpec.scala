package com.example

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.catsSyntaxEitherId
import com.example.domain.Ballot
import com.example.domain.Lottery
import com.example.domain.Principal
import com.example.domain.PrincipalId
import com.example.repository.LotterySystemRepository
import com.example.services.LotterySystemService
import com.example.testUtils.TestData
import io.circe.generic.auto._
import io.circe.syntax._
import org.mockito.scalatest.AsyncIdiomaticMockito
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode
import sttp.tapir.integ.cats.effect.CatsMonadError
import sttp.tapir.server.stub.TapirStubInterpreter

import java.util.UUID
import scala.annotation.nowarn

@nowarn
class LotterySystemSpec extends AsyncFlatSpec with AsyncIdiomaticMockito with AsyncIOSpec with Matchers {

  private val mockedRepository: LotterySystemRepository[IO] =
    mock[LotterySystemRepository[IO]]

  private val serverEndpoints =
    LotterySystemService.serverEndpoints(mockedRepository, TestData.apiKey)

  val backendStub: SttpBackend[IO, Any] =
    TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointsRunLogic(serverEndpoints)
      .backend()

  "POST /principals" should "enroll a new principal" in {

    val response = basicRequest
      .post(uri"http://localhost/principals")
      .send(backendStub)

    mockedRepository.createPrincipal(any[Principal]).returns(IO.pure(1))

    response.map { res =>
      res.code shouldBe StatusCode.Created
      res.body shouldBe a[Right[_, Principal]]
    }
  }

  "POST /lotteries/{lotteryId}/ballot" should "return Unauthorized if there is no Authorization header" in {

    val response = basicRequest
      .post(uri"http://localhost/lotteries/${TestData.lotteryId.value}/ballot")
      .send(backendStub)

    response.map(_.code shouldBe StatusCode.Unauthorized)
  }

  it should "return Unauthorized if the Principal is unknown" in {

    val response = basicRequest
      .post(uri"http://localhost/lotteries/${TestData.lotteryId.value}/ballot")
      .header("Authorization", s"Bearer ${UUID.randomUUID()}")
      .send(backendStub)

    mockedRepository
      .getPrincipal(any[PrincipalId])
      .returns(IO.raiseError(new Error))

    response.map(_.code shouldBe StatusCode.Unauthorized)
  }

  it should "return Created and put a ballot on the lottery" in {

    val response = basicRequest
      .post(uri"http://localhost/lotteries/${TestData.lotteryId.value}/ballot")
      .header("Authorization", s"Bearer ${TestData.principalId.value}")
      .send(backendStub)

    mockedRepository
      .getPrincipal(TestData.principalId)
      .returns(IO.pure(TestData.principal))
    mockedRepository
      .getLottery(TestData.lotteryId)
      .returns(IO.pure(TestData.lottery))
    mockedRepository.createBallot(any[Ballot]).returns(IO.pure(1))
    response.map(_.code shouldBe StatusCode.Created)
  }

  it should "return Forbidden if the lottery is closed" in {

    val response = basicRequest
      .post(
        uri"http://localhost/lotteries/${TestData.closedLotteryId.value}/ballot"
      )
      .header("Authorization", s"Bearer ${TestData.principalId.value}")
      .send(backendStub)

    mockedRepository
      .getPrincipal(TestData.principalId)
      .returns(IO.pure(TestData.principal))
    mockedRepository
      .getLottery(TestData.closedLotteryId)
      .returns(IO.pure(TestData.closedLottery))
    response.map(_.code shouldBe StatusCode.Forbidden)
  }

  private val endDateMinusOne = TestData.date.minusDays(1)

  private val returnedLotteries = List(
    TestData.lottery,
    TestData.closedLottery,
    TestData.lottery.copy(endDate = endDateMinusOne),
    TestData.closedLottery.copy(endDate = endDateMinusOne)
  )

  "GET /lotteries" should "return 200 with the list of lotteries" in {

    mockedRepository.getLotteries.returns(IO.pure(returnedLotteries))

    val response = basicRequest
      .get(uri"http://localhost/lotteries")
      .send(backendStub)

    response.map { res =>
      res.code shouldBe StatusCode.Ok
      res.body shouldBe returnedLotteries.asJson.noSpaces.asRight
    }
  }

  it should "return the list of lotteries filtered by the query params" in {

    mockedRepository.getLotteries.returns(IO.pure(returnedLotteries))

    val response1 = basicRequest
      .get(uri"http://localhost/lotteries?status=closed")
      .send(backendStub)

    response1.map { res =>
      res.code shouldBe StatusCode.Ok
      res.body shouldBe returnedLotteries
        .filter(_.winnerBallotId.isDefined)
        .asJson
        .noSpaces
        .asRight
    }

    val response2 = basicRequest
      .get(uri"http://localhost/lotteries?status=open")
      .send(backendStub)

    response2.map { res =>
      res.code shouldBe StatusCode.Ok
      res.body shouldBe returnedLotteries
        .filter(_.winnerBallotId.isEmpty)
        .asJson
        .noSpaces
        .asRight
    }

    val response3 = basicRequest
      .get(uri"http://localhost/lotteries?endDate=${endDateMinusOne.toString}")
      .send(backendStub)

    response3.map { res =>
      res.code shouldBe StatusCode.Ok
      res.body shouldBe returnedLotteries
        .filter(_.endDate.isEqual(endDateMinusOne))
        .asJson
        .noSpaces
        .asRight
    }

    val response4 = basicRequest
      .get(
        uri"http://localhost/lotteries?endDate=${endDateMinusOne.toString}&status=open"
      )
      .send(backendStub)

    response4.map { res =>
      res.code shouldBe StatusCode.Ok
      res.body shouldBe returnedLotteries
        .filter(_.endDate.isEqual(endDateMinusOne))
        .filter(_.winnerBallotId.isEmpty)
        .asJson
        .noSpaces
        .asRight
    }

  }

  "POST /admin/lotteries" should "return Unauthorized if there is no Authorization header" in {

    val response = basicRequest
      .post(uri"http://localhost/admin/lotteries")
      .send(backendStub)

    response.map(_.code shouldBe StatusCode.Unauthorized)
  }

  it should "return Unauthorized if the ApiKey is unknown" in {

    val response = basicRequest
      .post(uri"http://localhost/admin/lotteries")
      .header("Authorization", s"Bearer ${UUID.randomUUID()}")
      .send(backendStub)

    response.map(_.code shouldBe StatusCode.Unauthorized)
  }

  it should "return Created and open the lottery" in {

    val response = basicRequest
      .post(uri"http://localhost/admin/lotteries")
      .header("Authorization", s"Bearer ${TestData.apiKey}")
      .body(TestData.date.asJson)
      .send(backendStub)

    mockedRepository.createLottery(any[Lottery]).returns(IO.pure(1))
    response.map(_.code shouldBe StatusCode.Created)
  }

  "PUT /admin/lotteries/{lotteryId}/close" should "return Unauthorized if there is no Authorization header" in {

    val response = basicRequest
      .put(
        uri"http://localhost/admin/lotteries/${TestData.lotteryId.value}/close"
      )
      .send(backendStub)

    response.map(_.code shouldBe StatusCode.Unauthorized)
  }

  it should "return Unauthorized if the ApiKey is unknown" in {

    val response = basicRequest
      .put(
        uri"http://localhost/admin/lotteries/${TestData.lotteryId.value}/close"
      )
      .header("Authorization", s"Bearer ${UUID.randomUUID()}")
      .send(backendStub)

    response.map(_.code shouldBe StatusCode.Unauthorized)
  }

  it should "return Ok and close the lottery" in {

    val response = basicRequest
      .put(
        uri"http://localhost/admin/lotteries/${TestData.lotteryId.value}/close"
      )
      .header("Authorization", s"Bearer ${TestData.apiKey}")
      .send(backendStub)

    mockedRepository
      .getLottery(TestData.lotteryId)
      .returns(IO.pure(TestData.lottery))
    mockedRepository
      .getBallotsForLottery(TestData.lotteryId)
      .returns(IO.pure(List(TestData.ballot)))
    mockedRepository
      .closeLottery(TestData.lotteryId, TestData.ballotId)
      .returns(IO.pure(1))
    response.map(_.code shouldBe StatusCode.Ok)
  }

  it should "return Forbidden if there are no ballots for the lottery" in {

    val response = basicRequest
      .put(
        uri"http://localhost/admin/lotteries/${TestData.lotteryId.value}/close"
      )
      .header("Authorization", s"Bearer ${TestData.apiKey}")
      .send(backendStub)

    mockedRepository
      .getLottery(TestData.lotteryId)
      .returns(IO.pure(TestData.lottery))
    mockedRepository
      .getBallotsForLottery(TestData.lotteryId)
      .returns(IO.pure(List()))

    response.map(_.code shouldBe StatusCode.Forbidden)
  }

  it should "return Forbidden if the lottery is closed" in {

    val response = basicRequest
      .put(
        uri"http://localhost/admin/lotteries/${TestData.closedLotteryId.value}/close"
      )
      .header("Authorization", s"Bearer ${TestData.apiKey}")
      .send(backendStub)

    mockedRepository
      .getLottery(TestData.closedLotteryId)
      .returns(IO.pure(TestData.closedLottery))
    response.map(_.code shouldBe StatusCode.Forbidden)
  }

}
