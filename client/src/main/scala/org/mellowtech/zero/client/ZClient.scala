package org.mellowtech.zero.client

import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.{ActorSystem, Terminated}
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import org.mellowtech.zero.grpc.{ZNewTimer, ZSearchRequest, ZTimerRequest, ZeroService, ZeroServiceClient}
import org.mellowtech.zero.model.Timer

import scala.concurrent.Future

class ZClient(host: String, port: Int) {

  import org.mellowtech.zero.model.GrpcConverts._

  implicit val sys = ActorSystem("ZClient")
  implicit val mat = ActorMaterializer()
  implicit val ec = sys.dispatcher

  //For now programmatic...change to config based
  val clientSettings = GrpcClientSettings.connectToServiceAt(host, port).withTls(false)

  val client: ZeroService = ZeroServiceClient(clientSettings)

  def list: Future[Seq[Timer]] = {
    for {
      timers <- client.listTimers(ZSearchRequest())
    } yield timers.timer.map(toTimer(_))
  }

  def addTimer(title: String, start: OffsetDateTime = OffsetDateTime.now(), duration: Either[OffsetDateTime, Long],
               description: String = ""): Future[Timer] = {
    require(title != null && title.length > 0)
    val req = duration match {
      case Left(x) => ZNewTimer(title = title, start = start.toString(), stop = x.toString, desc = description)
      case Right(x) => ZNewTimer(title = title, start = start.toString(), millis = x, desc = description)
    }
    client.createTimer(req).map(toTimer(_))
  }

  def getTimer(id: UUID): Future[Timer] = {
    import org.mellowtech.zero.grpc.ZTimerRequest.Critera.Uuid
    val req = ZTimerRequest(Uuid(id.toString))
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
