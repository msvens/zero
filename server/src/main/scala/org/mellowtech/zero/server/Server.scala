package org.mellowtech.zero.server


import java.time.OffsetDateTime

import akka.actor.{ActorSystem, Terminated}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.{ActorMaterializer, Materializer}
import org.mellowtech.zero.model.SuccessFailure._
import org.mellowtech.zero.model._
import org.mellowtech.zero.util.TimerFuncs.{elapsedFull, elapsedSeconds, elapsedToDays, remainingFull, remainingSeconds, remainingToDays, utcNow}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

/**
  * @author msvens
  * @since 01/10/16
  */

class ZeroServer()(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends Config{

  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)

  //setup database
  val dbService = new DbService()
  val timerDAO = new TimerDAO(dbService)

  val binding = Http().bindAndHandle(route, httpHost, httpPort)

  // report successful binding
  binding.foreach { binding =>
    println(s"server bound to: ${binding.localAddress}")
  }



  def shutdown(): Future[Terminated] ={
    import scala.concurrent.duration._

    Await.result(binding, 10.seconds)
      .terminate(hardDeadline = 3.seconds).flatMap(_ => {
      //dbService.dataSource.close
      actorSystem.terminate()
    })
  }

  def route(implicit m: Materializer): Route = {

    import Directives._
    import org.mellowtech.zero.model.JsonCodecs._
    import de.heikoseeberger.akkahttpjsoniterscala.JsoniterScalaSupport._

    pathPrefix("timers") {
      log.debug("found timers path...")
      pathEndOrSingleSlash {
        get {
          log.debug("list timers")
          onSuccess(timerDAO.list) {
            tt => complete(TimersResponse(SUCCESS, Some("listTimers"), Some(tt)))
          }
        } ~
          post {
            log.debug("post timer")
            entity(as[AddTimer]) { at =>
              val end: Either[OffsetDateTime, Long] = at.stop match {
                case Some(odt) => Left(odt)
                case None => Right(at.millis.getOrElse(1000*3600*24))
              }
              val f = for {
                id <- timerDAO.insert(at.title, at.start.getOrElse(utcNow), end, at.desc)
                tt <- timerDAO.get(id)
              } yield TimerResponse(SUCCESS, Some("addTimers"), tt)
              onSuccess(f) {
                ft => complete(ft)
              }
            }
          }
      } ~
        pathPrefix(JavaUUID) { id =>
          pathEndOrSingleSlash {
            log.debug("get timer")
            get {
              onSuccess(timerDAO.get(id)) {
                case Some(timer) =>
                  log.debug("successfully received timer")
                  complete(TimerResponse(SUCCESS, Some("getTimer"), Some(timer)))

                case None => complete(TimerResponse(ERROR, Some("getTimer"), None))
              }
            }
          } ~
            pathPrefix("elapsed") {
              pathEndOrSingleSlash {
                get {
                  onSuccess(timerDAO.get(id)) {
                    case Some(timer) => complete(CounterResponse(SUCCESS, Some("elapsedFull"), Some(elapsedFull(timer))))
                    case None => complete(CounterResponse(ERROR, Some("elapsedFull"), None))
                  }
                }
              } ~
                pathSuffix("days") {
                  get {
                    onSuccess(timerDAO.get(id)) {
                      case Some(timer) => complete(CounterResponse(SUCCESS, Some("elapsedDays"), Some(elapsedToDays(timer))))
                      case None => complete(CounterResponse(ERROR, Some("elapsedDays"), None))
                    }
                  }
                } ~
                pathSuffix("seconds") {
                  get {
                    onSuccess(timerDAO.get(id)) {
                      case Some(timer) => complete(CounterResponse(SUCCESS, Some("elapsedSeconds"), Some(elapsedSeconds(timer))))
                      case None => complete(CounterResponse(ERROR, Some("elapsedSeconds"), None))
                    }
                  }
                }
            } ~
            pathPrefix("remaining") {
              pathEndOrSingleSlash {
                get {
                  onSuccess(timerDAO.get(id)) {
                    case Some(timer) => complete(CounterResponse(SUCCESS, Some("remainingFull"), Some(remainingFull(timer))))
                    case None => complete(CounterResponse(ERROR, Some("remainingFull"), None))
                  }
                }
              } ~
                pathSuffix("days") {
                  get {
                    onSuccess(timerDAO.get(id)) {
                      case Some(timer) => complete(CounterResponse(SUCCESS, Some("remainingDays"), Some(remainingToDays(timer))))
                      case None => complete(CounterResponse(ERROR, Some("remainingDays"), None))
                    }
                  }
                } ~
                pathSuffix("seconds") {
                  get {
                    onSuccess(timerDAO.get(id)) {
                      case Some(timer) => complete(CounterResponse(SUCCESS, Some("remainingSeconds"), Some(remainingSeconds(timer))))
                      case None => complete(CounterResponse(ERROR, Some("remainingSeconds"), None))
                    }
                  }
                }
            }
        }
    }
  }

}

object ZeroServer {

  def apply(): ZeroServer = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    new ZeroServer()
  }

}

object Server extends App {

  val server = ZeroServer()
  StdIn.readLine()
  server.shutdown()
}