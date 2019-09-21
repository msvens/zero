package org.mellowtech.zero.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mellowtech.zero.grpc.{ZNewTimer, ZSearchRequest, ZTimer, ZTimerRequest}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.scalatest.{Assertion, BeforeAndAfter, BeforeAndAfterAll}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

class ZeroServiceImplSpec extends AsyncFlatSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll {


  var timerDAO: TimerDAO = _

  val system: ActorSystem = ActorSystem("ZeroServiceSystem")
  val mat = ActorMaterializer.create(system)

  //val timerDAO = new TimerDAO(DatabaseConfig.forConfig[JdbcProfile]("h2mem_server_dc"))

  //val service = new ZeroServiceImpl(timerDAO)
  var service: ZeroServiceImpl = _

  before {
    timerDAO = new TimerDAO(DatabaseConfig.forConfig[JdbcProfile]("h2mem_server_dc"))
    timerDAO.createTables()
    service = new ZeroServiceImpl(timerDAO)(mat)
  }

  after {
    timerDAO.db.close()
  }

  override def afterAll(): Unit = {
    Await.ready(system.terminate(), 5.seconds)
  }

  def newZTimer(): ZNewTimer = {
    ZNewTimer(title = "timer", start = None, zoneId = "UTC", duration = ZNewTimer.Duration.Millis(36000), desc = "description")
  }

  def assertDefaultTimer(zt: ZTimer): Assertion = {
    assert(zt.title == "timer")
    assert(zt.desc == "description")
  }

  behavior of "ZeroServiceImpl"

  it should "create a new timer" in {
    service.createTimer(newZTimer()).map(assertDefaultTimer(_))
  }

  it should "retrieve a timer" in {
    for {
      t <- service.createTimer(newZTimer())
      tt <- service.getTimer(ZTimerRequest(ZTimerRequest.Critera.Id(t.id)))
    } yield {
      assert(tt.timer.size == 1)
      assertDefaultTimer(tt.timer.head)
    }
  }

  it should "list all timers" in {
    for {
      t <- service.createTimer(newZTimer())
      tt <- service.createTimer(newZTimer())
      timers <- service.listTimers(ZSearchRequest())
    } yield {
      assert(timers.timer.size == 2)
    }
  }



}
