package org.mellowtech.zero.client

import java.time.{Clock, Instant}

import akka.actor.{ActorSystem, Terminated}
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import org.mellowtech.zero.grpc.CounterType._
import org.mellowtech.zero.grpc.{AddSplitRequest, AddTimerRequest, GetCounterRequest, GetSplitRequest, GetTimerByIdRequest, ListTimersRequest, ZeroService, ZeroServiceClient}
import org.mellowtech.zero.model.{Counter, Split, Timer}

import scala.concurrent.Future

class ZClient(host: String, port: Int) {

  import org.mellowtech.zero.model.GrpcConverts._

  implicit val sys = ActorSystem("ZClient")
  implicit val mat = ActorMaterializer()
  implicit val ec = sys.dispatcher
  val clock = Clock.systemDefaultZone()

  //For now programmatic...change to config based
  val clientSettings = GrpcClientSettings.connectToServiceAt(host, port).withTls(false)

  val client: ZeroService = ZeroServiceClient(clientSettings)

  def list: Future[Seq[Timer]] = {
    for {
      timers <- client.listTimers(ListTimersRequest())
    } yield timers.timers.map(toTimer)
  }

  def counter(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(GetCounterRequest(id, remaining, YEARS))
  }

  def millis(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(GetCounterRequest(id, true, MILLIS))
  }

  def seconds(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(GetCounterRequest(id, true, SECONDS))
  }

  def minutes(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(GetCounterRequest(id, true, MINUTES))
  }

  def hours(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(GetCounterRequest(id, true, HOURS))
  }

  def days(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(GetCounterRequest(id, true, DAYS))
  }

  def months(id: Long, remaining: Boolean = false): Future[Counter] = getCounter(GetCounterRequest(id, true, MONTHS))

  private def getCounter(req: GetCounterRequest): Future[Counter] = {
    client.getCounter(req).map(resp => {
      if(resp.counter.isEmpty) throw new Exception("No Timer with that ID "+req.timerId)
      else toCounter(resp.counter.get, resp.counterType)
    })
  }

  def addTimer(name: String, start: Instant = Instant.now(), duration: Either[Instant, Long],
               description: String = ""): Future[Timer] = {
    require(name != null && name.length > 0)
    val d = duration match {
      case Left(x) => AddTimerRequest.Duration.Stop(toZInstant(x))
      case Right(x) => AddTimerRequest.Duration.Millis(x)
    }
    val req = AddTimerRequest(title = name, start = Some(toZInstant(start)), duration = d, zoneId = clock.getZone.getId, desc = description)
    client.addTimer(req).map(resp => {toTimer(resp.getTimer)})
  }

  def addSplit(id: Long, description: String = ""): Future[Split] = {
    val req = AddSplitRequest(timer = id, description = description)
    client.addSplit(req).map(resp => {toSplit(resp.getSplit)})
  }

  def getSplits(timer: Long): Future[Seq[Split]] = for{
    resp <- client.getSplit(GetSplitRequest(timer))
  } yield resp.splits.map(toSplit)

  def getTimer(id: Long): Future[Timer] = {
    client.getTimerById(GetTimerByIdRequest(id)).map(resp => {
      if(resp.timers.isEmpty) throw new Exception("no such user")
      else
        toTimer(resp.timers.head)
    })
  }



  def close(): Future[Terminated] = {
    sys.terminate()
  }

}
