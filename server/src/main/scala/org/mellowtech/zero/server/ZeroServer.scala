package org.mellowtech.zero.server

import java.time.{Instant, ZoneId}

import akka.actor.{ActorSystem, Terminated}
import akka.event.{Logging, LoggingAdapter}
import akka.grpc.GrpcServiceException
import akka.http.scaladsl.Http2
import akka.http.scaladsl.HttpConnectionContext
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, Materializer}
import io.grpc.Status
import org.mellowtech.zero.grpc._
import org.mellowtech.zero.model.Timer
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

object ZeroGrpcServer extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("GrpcServer")

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val timerDAO = new TimerDAO(DatabaseConfig.forConfig[JdbcProfile]("pg_dc"))(actorSystem.dispatcher)

  val server = new ZeroGrpcServer(timerDAO)
  println("server started!")
  StdIn.readLine()
  server.shutdown()

}

class ZeroGrpcServer(val timerDAO: TimerDAO)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends Config {

  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)

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
      Future.failed(new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("title cannot be empty")))
    } else {

      val duration = parseDuration(in)
      val description = toOption(in.desc)
      val zone = parseZoneId(in)
      val start = parseStart(in)
      val user = toOption(in.user)

      for {
        tt <- timerDAO.addTimer(user, in.title, start, duration, zone, description)
      } yield toZTimer(tt)
    }
  }

  override def getTimer(in: ZTimerRequest): Future[ZTimerResponse] = {
    if(in.critera.isId){
      val id = in.critera.id.get
      timerDAO.get(id).map {
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

  override def listTimers(in: ZSearchRequest): Future[ZTimerResponse] = for {
    timers <- timerDAO.list
  } yield ZTimerResponse(timers.map(toZTimer(_)))

  override def getCounter(in: ZCounterRequest): Future[ZCounterResponse] = {
    import org.mellowtech.zero.util.TimerFuncs._

    def ctr(t: Timer, ct: ZCounterType, remaining: Boolean): ZCounterResponse = {
      try {
        val c = counter(t, toUnits(ct), remaining)
        ZCounterResponse(Some(toZCounter(c)), ct)
      } catch {
        case iae: IllegalArgumentException =>
          throw new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("To and From are not correct"))
        case e: Exception =>
          throw new GrpcServiceException(Status.INTERNAL.withDescription("Error calculating counter"))
      }

    }
    val ct = in.counterType
    //val units = toUnits(ct)
    val timer = timerDAO.get(in.id)
    timer.map {
      case None => ZCounterResponse(None,ct)
      case Some(t) => {
        ctr(t, ct, in.remaining)
      }
    }
  }

  override def addSplit(in: ZNewSplit): Future[ZSplit] = {
    val instant = in.time.isDefined match {
      case true => toInstant(in.time.get)
      case false => Instant.now()
    }
    val description = in.description match {
      case "" => None
      case _ => Some(in.description)
    }
    timerDAO.addSplit(in.timer, instant, description).map(s => toZSplit(s))
  }

  override def getSplits(in: ZSplitRequest): Future[ZSplitResponse] = for {
    splits <- timerDAO.getSplits(in.timer)
  } yield ZSplitResponse(splits.map(toZSplit(_)))

  override def createUser(in: ZNewUser): Future[ZUser] = {
    ???
  }

  override def getUser(in: ZUserRequest): Future[ZUser] = {
    ???
  }
}
