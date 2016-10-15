package org.mellowtech.zero.server

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.{ActorMaterializer, Materializer}
import org.json4s.native
import org.mellowtech.zero.model.SuccessFailure._
import org.mellowtech.zero.model.{AddTimer, ApiResponse, Counter, Timer}
import org.mellowtech.zero.util.Implicits

import scala.concurrent.ExecutionContext

/**
  * @author msvens
  * @since 01/10/16
  */
object Server extends Config {

  import org.mellowtech.zero.util.TimerFuncs._

  implicit val actorSystem = ActorSystem()
  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  //implicit val formats =
  //implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  def main(args: Array[String]): Unit = {

    Http().bindAndHandle(route, httpHost, httpPort)
  }

  def route(implicit m: Materializer) = {
    val dbService = new DbService(jdbcUrl, dbUser, dbPassword)
    val timerDAO = new TimerDAO(dbService)

    import Directives._
    import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

    implicit val serialization = native.Serialization
    // or native.Serialization
    implicit val formats = Implicits.formats

    pathPrefix("timers") {
      log.debug("found timers path...")
      pathEndOrSingleSlash {
        get {
          log.debug("list timers")
          onSuccess(timerDAO.list) {
            tt => complete(ApiResponse(SUCCESS, Some("listTimers"), Some(tt)))
          }
        } ~
          post {
            log.debug("post timer")
            entity(as[AddTimer]) { at =>
              val start = at.start.getOrElse(OffsetDateTime.now())
              val stop = if (at.seconds.isDefined) {
                start.plus(at.seconds.get, ChronoUnit.SECONDS)
              } else {
                at.stop.getOrElse(start.plusYears(1))
              }
              val t = Timer(None, at.title, Some(start), Some(stop), at.desc)
              val f = for {
                id <- timerDAO.insert(t)
                tt <- timerDAO.get(id.get)
              } yield ApiResponse(SUCCESS, Some("addTimers"), Some(tt))
              onSuccess(f) {
                ft => complete(ft)
              }
            }
          }
      } ~
        pathPrefix(IntNumber) { id =>
          pathEndOrSingleSlash {
            log.debug("get timer")
            get {
              onSuccess(timerDAO.get(id)) {
                case Some(timer) => {
                  log.debug("successfully received timer")
                  complete(ApiResponse[Timer](SUCCESS, Some("getTimer"), Some(timer)))
                }
                case None => complete(ApiResponse[Timer](ERROR, Some("getTimer"), None))
              }
            }
          } ~
            pathPrefix("elapsed") {
              pathEndOrSingleSlash {
                get {
                  onSuccess(timerDAO.get(id)) {
                    case Some(timer) => complete(ApiResponse[Counter](SUCCESS, Some("elapsedFull"), Some(elapsedFull(timer))))
                    case None => complete(ApiResponse[Counter](ERROR, Some("elapsedFull"), None))
                  }
                }
              } ~
                pathSuffix("days") {
                  get {
                    onSuccess(timerDAO.get(id)) {
                      case Some(timer) => complete(ApiResponse[Counter](SUCCESS, Some("elapsedDays"), Some(elapsedToDays(timer))))
                      case None => complete(ApiResponse[Counter](ERROR, Some("elapsedDays"), None))
                    }
                  }
                } ~
                pathSuffix("seconds") {
                  get {
                    onSuccess(timerDAO.get(id)) {
                      case Some(timer) => complete(ApiResponse[Counter](SUCCESS, Some("elapsedSeconds"), Some(elapsedSeconds(timer))))
                      case None => complete(ApiResponse[Counter](ERROR, Some("elapsedSeconds"), None))
                    }
                  }
                }
            } ~
            pathPrefix("remaining") {
              pathEndOrSingleSlash {
                get {
                  onSuccess(timerDAO.get(id)) {
                    case Some(timer) => complete(ApiResponse[Counter](SUCCESS, Some("remainingFull"), Some(remainingFull(timer))))
                    case None => complete(ApiResponse[Counter](ERROR, Some("remainingFull"), None))
                  }
                }
              } ~
                pathSuffix("days") {
                  get {
                    onSuccess(timerDAO.get(id)) {
                      case Some(timer) => complete(ApiResponse[Counter](SUCCESS, Some("remainingDays"), Some(remainingToDays(timer))))
                      case None => complete(ApiResponse[Counter](ERROR, Some("remainingDays"), None))
                    }
                  }
                } ~
                pathSuffix("seconds") {
                  get {
                    onSuccess(timerDAO.get(id)) {
                      case Some(timer) => complete(ApiResponse[Counter](SUCCESS, Some("remainingSeconds"), Some(remainingSeconds(timer))))
                      case None => complete(ApiResponse[Counter](ERROR, Some("remainingSeconds"), None))
                    }
                  }
                }
            }
        }
    }
  }

}
