package org.mellowtech.zero.server

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import org.mellowtech.zero.db.TimerDAO
import org.mellowtech.zero.grpc.{AddTimerRequest, TimerItem, ZeroServiceClient}
import org.scalatest.{Assertion, BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

class ZeroGrpcServerSpec extends AsyncFlatSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll {


  //Server
  val timerDAO: TimerDAO = TimerDAO("h2mem_server_dc")
  val serverSystem = ActorSystem("ZeroServerSystem")
  val serverSystemMat = ActorMaterializer.create(serverSystem)
  val server = new ZeroGrpcServer(timerDAO)(serverSystem, serverSystemMat)

  //Client
  implicit val clientSystem = ActorSystem("ZeroClientSystem")
  implicit val mat = ActorMaterializer()

  val client = {
    implicit val ec = clientSystem.dispatcher
    val clientSettings = GrpcClientSettings.connectToServiceAt("localhost", 9090).withTls(false)
    ZeroServiceClient(clientSettings)
  }

  before {
    timerDAO.createTablesSynced()
  }

  after {
    Await.ready(timerDAO.dropTables(), 5.seconds)
  }

  override def afterAll(): Unit = {
    Await.ready(clientSystem.terminate(), 5.seconds)
    Await.ready(server.shutdown(), 5.seconds)
    timerDAO.db.close()
  }

  def newTimerItem(): AddTimerRequest = {
    AddTimerRequest(title = "timer", start = None, zoneId = "UTC", duration = AddTimerRequest.Duration.Millis(36000), desc = "description")
  }

  def assertDefaultTimer(ti: TimerItem): Assertion = {
    assert(ti.title == "timer")
    assert(ti.desc == "description")
  }

  behavior of "ZeroGrpcServer"

  it should "create a new timer" in {
    client.addTimer(newTimerItem()).map(resp => assertDefaultTimer(resp.getTimer))
  }

}
