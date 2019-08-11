package org.mellowtech.zero.server

import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.{ActorSystem, Terminated}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.UseHttp2.Always
import akka.http.scaladsl.Http2
import akka.http.scaladsl.HttpConnectionContext
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import org.mellowtech.zero.grpc._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

object ZeroGrpcServer extends App {

  val conf = ConfigFactory
    .parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())

  implicit val actorSystem: ActorSystem = ActorSystem("GrpcServer", conf)

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val server = new ZeroGrpcServer()
  println("server started!")
  StdIn.readLine()
  server.shutdown()

}

class ZeroGrpcServer()(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends Config {

  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)

  //setup database
  val dbService = new DbService()
  val timerDAO = new TimerDAO(dbService)

  //grpc service
  val service: HttpRequest => Future[HttpResponse] =
    ZeroServiceHandler(new ZeroServiceImpl(timerDAO))

  val binding = Http2().bindAndHandleAsync(
    service,
    interface = "localhost",
    port = httpPort,
    connectionContext = HttpConnectionContext())

  // report successful binding
  binding.foreach { binding =>
    println(s"gRPC server bound to: ${binding.localAddress}")
  }



  def shutdown(): Future[Terminated] ={
    import scala.concurrent.duration._

    Await.result(binding, 10.seconds)
      .terminate(hardDeadline = 3.seconds).flatMap(_ => {
      //dbService.dataSource.close
      actorSystem.terminate()
    })
  }

}

class ZeroServiceImpl(timerDAO: TimerDAO)(implicit mat: Materializer) extends ZeroService {

  import mat.executionContext
  import org.mellowtech.zero.model.GrpcConverts._

  override def createTimer(in: ZNewTimer): Future[ZTimer] = {
    if(in.title == "") {
      Future.failed(new Exception("Missing title"))
    } else {
      val end: Either[OffsetDateTime, Long] = in.stop match {
        case "" => in.millis match {
          case 0 => Right(1000*3600*24)
          case _ => Right(in.millis)
        }
        case _ => Left(OffsetDateTime.parse(in.stop))
      }
      val start = in.start match {
        case "" => OffsetDateTime.now()
        case _ => OffsetDateTime.parse(in.start)
      }
      val description = in.desc match {
        case "" => None
        case _ => Some(in.desc)
      }
      for {
        id <- timerDAO.insert(in.title, start, end, description)
        tt <- timerDAO.get(id)
      } yield toZTimer(tt)
    }
  }

  override def getTimer(in: ZTimerRequest): Future[ZTimerResponse] = {
    if(in.critera.isUuid){
      val uuid = UUID.fromString(in.critera.uuid.get)
      timerDAO.get(uuid).map {
        case Some(t) => {
          ZTimerResponse(Seq(toZTimer(t)))
        }
        case None => ZTimerResponse()
      }
    } else if(in.critera.isTitle) {
      timerDAO.get(in.critera.title.get).map {
        case Some(t) => ZTimerResponse(Seq(toZTimer(t)))
        case None => ZTimerResponse()
      }
    } else
      Future.failed(new Exception("Unimplemented criteria"))
  }

  override def getCounter(in: ZCounterRequest): Future[ZCounter] = {
    import org.mellowtech.zero.util.TimerFuncs._
    val units = toUnits(in.counters)
    val timer = timerDAO.get(UUID.fromString(in.uuid))
    timer.map {
      case None => ZCounter()
      case Some(t) => in.remaining match {
        case true => zremaining(t, units)
        case false => zelapsed(t, units)
      }
    }
  }

  override def listTimers(in: ZSearchRequest): Future[ZTimerResponse] = for {
    timers <- timerDAO.list
  } yield ZTimerResponse(timers.map(toZTimer(_)))
}
