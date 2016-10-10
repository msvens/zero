package org.mellowtech.zero.client

import java.net.URI
import scala.concurrent.Await
import scala.concurrent.duration._

case class TConfig(cmd: String = "", url: String = "", title: String = "", id: Int = -1, days:
  Boolean = false, seconds: Boolean = false)

/**
  * @author msvens
  * @since 17/09/16
  */
object TimeCmd extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val parser = new scopt.OptionParser[TConfig]("timer") {
    head("timer", "1.0")

    opt[Int]('i',"timerId").action((i,tc) =>
      tc.copy(id = i)).text("id of timer")

    arg[URI]("<uri>").action((u,tc) =>
      tc.copy(url = u.toString)).required().text("base url of timer service")

    help("help").text("prints this usage text")

    cmd("list").action((_,tc) => tc.copy(cmd = "list")).text("list available timers")
    cmd("add").action((_,tc) => tc.copy(cmd = "add")).text("add new timer").children(
      opt[String]("title").abbr("t").action( (x, tc) =>
        tc.copy(title = x) ).text("title of the new timer")
    )
    cmd("get").action((_,tc) => tc.copy(cmd = "get")).text("get timer")
    cmd("elapsed").action((_,tc) => tc.copy(cmd = "elapsed")).text("get elapsed time for timer").
      children(
        opt[Unit]("days").abbr("d").action( (_, tc) =>
          tc.copy(days = true) ).text("elapsed days, hours, minute and seconds"),
        opt[Unit]("seconds").abbr("s").action( (_, tc) =>
          tc.copy(seconds = true) ).text("elapsed seconds")
      )
    cmd("remaining").action((_,tc) => tc.copy(cmd = "remaining")).text("get remaining time for timer").
    children(
      opt[Unit]("days").abbr("d").action( (_, tc) =>
        tc.copy(days = true) ).text("remaining days, hours, minute and seconds"),
      opt[Unit]("seconds").abbr("s").action( (_, tc) =>
        tc.copy(seconds = true) ).text("remaining seconds")
    )
    checkConfig(tc => tc match {
      case TConfig("",_,_,_,_,_) => failure("no command given")
      case _ => success
    })
  }



  parser.parse(args, TConfig()) match {
    case Some(config) => {
      val c = Client(config.url)
      config.cmd match {
        case "list" => {
          val l = Await.result(c.list, 2 seconds)
          println(l mkString "\n")
        }
        case "get" => {
          val t = Await.result(c.get(config.id), 2 seconds)
          println(t)
        }
        case "add" => {
          //
        }
        case _ => {
          println("command: "+config.cmd+" not yet implemented")
        }
      }
      c.close
    }
    case None => {
      println("not valid config")
    }
  }

  sys.exit(0)


}
