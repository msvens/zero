package org.mellowtech.zero.server

import java.time.Instant

import akka.actor.{ActorSystem, Terminated}
import akka.event.{Logging, LoggingAdapter}
import akka.grpc.GrpcServiceException
import akka.http.scaladsl.Http2
import akka.http.scaladsl.HttpConnectionContext
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, Materializer}
import io.grpc.Status
import org.mellowtech.zero.db.TimerDAO
import org.mellowtech.zero.grpc._
import org.mellowtech.zero.model.Timer

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

object ZeroGrpcServer extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("GrpcServer")

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val timerDAO = TimerDAO("pg_dc")(actorSystem.dispatcher)

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

  override def addTimer(in: AddTimerRequest): Future[AddTimerResponse] = {

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
      } yield AddTimerResponse(Some(toTimerItem(tt)))
    }
  }

  override def getTimerById(in: GetTimerByIdRequest): Future[GetTimerResponse] = {
    timerDAO.getTimer(in.id).map {
      case Some(t) => GetTimerResponse(Seq(toTimerItem(t)))
      case None => GetTimerResponse()
    }
  }

  override def getTimerByName(in: GetTimerByNameRequest): Future[GetTimerResponse] = {
    timerDAO.getTimerByTitle(in.name).map {
      case Some(t) => GetTimerResponse(Seq(toTimerItem(t)))
      case None => GetTimerResponse()
    }
  }

  override def getTimerByUser(in: GetTimerByUserRequest): Future[GetTimerResponse] = for {
    timers <- timerDAO.getTimersByUser(in.userId)
  } yield GetTimerResponse(timers.map(toTimerItem))

  override def listTimers(in: ListTimersRequest): Future[ListTimersResponse] = for {
    timers <- timerDAO.listTimers()
  } yield ListTimersResponse(timers.map(toTimerItem))

  override def getCounter(in: GetCounterRequest): Future[GetCounterResponse] = {
    import org.mellowtech.zero.util.TimerFuncs._

    def ctr(t: Timer, ct: CounterType, remaining: Boolean): GetCounterResponse = {
      try {
        val c = counter(t, toUnits(ct), remaining)
        GetCounterResponse(Some(toCounterItem(c)), ct)
      } catch {
        case iae: IllegalArgumentException =>
          throw new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("To and From are not correct"))
        case e: Exception =>
          throw new GrpcServiceException(Status.INTERNAL.withDescription("Error calculating counter"))
      }

    }
    val ct = in.counterType
    //val units = toUnits(ct)
    val timer = timerDAO.getTimer(in.timerId)
    timer.map {
      case None => GetCounterResponse(None,ct)
      case Some(t) => ctr(t, ct, in.remaining)
    }
  }

  override def addSplit(in: AddSplitRequest): Future[AddSplitResponse] = {
    val instant = if (in.time.isDefined) {
      toInstant(in.time.get)
    } else {
      Instant.now()
    }
    val description = in.description match {
      case "" => None
      case _ => Some(in.description)
    }
    timerDAO.addSplit(in.timer, instant, description).map(s => AddSplitResponse(Some(toSplitItem(s))))
  }

  override def getSplit(in: GetSplitRequest): Future[GetSplitResponse] = for {
    splits <- timerDAO.getSplits(in.timerId)
  } yield GetSplitResponse(splits.map(toSplitItem))

  override def addUser(in: AddUserRequest): Future[AddUserResponse] = for {
    user <- timerDAO.addUser(in.username, in.email, None)
  } yield AddUserResponse(Some(toUserItem(user)))

  override def getUserById(in: GetUserByIdRequest): Future[GetUserResponse] = {
    timerDAO.getUser(in.id).map {
      case Some(u) => GetUserResponse(Some(toUserItem(u)))
      case None => GetUserResponse(None)
    }
  }

  override def getUserByName(in: GetUserByNameRequest): Future[GetUserResponse] = {
    timerDAO.getUserByName(in.name).map {
      case Some(u) => GetUserResponse(Some(toUserItem(u)))
      case None => GetUserResponse(None)
    }
  }

  override def listUsers(in: ListUsersRequest): Future[ListUsersResponse] = for {
    users <- timerDAO.listUsers()
  } yield ListUsersResponse(users.map(toUserItem))
}
