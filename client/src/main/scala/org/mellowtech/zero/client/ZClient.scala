package org.mellowtech.zero.client

import java.time.{Clock, Instant}

import akka.actor.{ActorSystem, Terminated}
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import org.mellowtech.zero.grpc.{ZCounterRequest, ZCounterType, ZNewSplit, ZNewTimer, ZSearchRequest, ZSplitRequest, ZTimerRequest, ZeroService, ZeroServiceClient}
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
      timers <- client.listTimers(ZSearchRequest())
    } yield timers.timer.map(toTimer)
  }

  def counter(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(ZCounterRequest(id, remaining, ZCounterType.YEARS))
  }

  def millis(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(ZCounterRequest(id, true, ZCounterType.MILLIS))
  }

  def seconds(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(ZCounterRequest(id, true, ZCounterType.SECONDS))
  }

  def minutes(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(ZCounterRequest(id, true, ZCounterType.MINUTES))
  }

  def hours(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(ZCounterRequest(id, true, ZCounterType.HOURS))
  }

  def days(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(ZCounterRequest(id, true, ZCounterType.DAYS))
  }

  def months(id: Long, remaining: Boolean = false): Future[Counter] = {
    getCounter(ZCounterRequest(id, true, ZCounterType.MONTHS))
  }

  private def getCounter(req: ZCounterRequest): Future[Counter] = {
    client.getCounter(req).map(resp => {
      if(resp.counter.isEmpty) throw new Exception("No Timer with that ID "+req.id)
      else toCounter(resp.counter.get, resp.counterType)
    })
  }

  def addTimer(name: String, start: Instant = Instant.now(), duration: Either[Instant, Long],
               description: String = ""): Future[Timer] = {
    require(name != null && name.length > 0)
    val d = duration match {
      case Left(x) => ZNewTimer.Duration.Stop(toZInstant(x))
      case Right(x) => ZNewTimer.Duration.Millis(x)
    }
    val req = ZNewTimer(title = name, start = Some(toZInstant(start)), duration = d, zoneId = clock.getZone.getId, desc = description)
    client.createTimer(req).map(toTimer(_))
  }

  def addSplit(id: Long, description: String = ""): Future[Split] = {
    val req = ZNewSplit(timer = id, description = description)
    client.addSplit(req).map(toSplit(_))
  }

  def getSplits(timer: Long): Future[Seq[Split]] = for{
    resp <- client.getSplits(ZSplitRequest(timer))
  } yield resp.split.map(toSplit(_))

  def getTimer(id: Long): Future[Timer] = {
    import org.mellowtech.zero.grpc.ZTimerRequest.Critera.Id
    val req = ZTimerRequest(Id(id))
    val resp: Future[Timer] = client.getTimer(req).map(x => {
      if(x.timer.isEmpty)
        throw new Exception("no such user")
      else
        toTimer(x.timer(0))
    })
    resp
  }



  def close(): Future[Terminated] = {
    sys.terminate()
  }

}
