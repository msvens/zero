package org.mellowtech.zero.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mellowtech.zero.db.TimerDAO
import org.mellowtech.zero.grpc.{AddTimerRequest, GetTimerByIdRequest, ListTimersRequest, TimerItem}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.scalatest.{Assertion, BeforeAndAfter, BeforeAndAfterAll}

class ZeroServiceImplSpec extends AsyncFlatSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll {


  var timerDAO: TimerDAO = _

  val system: ActorSystem = ActorSystem("ZeroServiceSystem")
  val mat: ActorMaterializer = ActorMaterializer.create(system)

  var service: ZeroServiceImpl = _

  before {
    timerDAO = TimerDAO("h2mem_server_dc")
    timerDAO.createTablesSynced()
    service = new ZeroServiceImpl(timerDAO)(mat)
  }

  after {
    timerDAO.db.close()
  }

  override def afterAll(): Unit = {
    timerDAO.db.close()
    Await.ready(system.terminate(), 5.seconds)
  }

  def newTimerItem(): AddTimerRequest = {
    AddTimerRequest(title = "timer", start = None, zoneId = "UTC", duration = AddTimerRequest.Duration.Millis(36000), desc = "description")
  }

  def assertDefaultTimer(ti: TimerItem): Assertion = {
    assert(ti.title == "timer")
    assert(ti.desc == "description")
  }

  behavior of "ZeroServiceImpl"

  it should "create a new timer" in {
    service.addTimer(newTimerItem()).map(resp => {assertDefaultTimer(resp.getTimer)})
  }

  it should "retrieve a timer" in {
    for {
      t <- service.addTimer(newTimerItem())
      tt <- service.getTimerById(GetTimerByIdRequest(t.getTimer.id))
    } yield {
      assert(tt.timers.size == 1)
      assertDefaultTimer(tt.timers.head)
    }
  }

  it should "list all timers" in {
    for {
      _ <- service.addTimer(newTimerItem())
      _ <- service.addTimer(newTimerItem())
      timers <- service.listTimers(ListTimersRequest())
    } yield {
      assert(timers.timers.size == 2)
    }
  }



}
