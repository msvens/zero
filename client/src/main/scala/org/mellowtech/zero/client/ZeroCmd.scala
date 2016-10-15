package org.mellowtech.zero.client

import java.net.URI

import org.mellowtech.zero.model.{AddTimer, Counter}

import scala.concurrent.Await
import scala.concurrent.duration._

case class TConfig(cmd: String = "", url: String = "", title: Option[String] = None, id: Int = -1, fmt: String = "f", seconds: Option[Int] = None)

/**
  * @author msvens
  * @since 17/09/16
  */
object TimeCmd extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val parser = new scopt.OptionParser[TConfig]("timer") {
    head("timer", "1.0")

    arg[URI]("<uri>").action((u,tc) =>
      tc.copy(url = u.toString)).required().text("base url of timer service")

    help("help").text("prints this usage text")

    cmd("list").action((_,tc) => tc.copy(cmd = "list")).text("list available timers")
    cmd("add").action((_,tc) => tc.copy(cmd = "add")).text("add new timer").children(
      opt[String]("title").abbr("t").required().action( (x, tc) =>
        tc.copy(title = Some(x)) ).text("title of the new timer"),
      opt[Int]("seconds").abbr("s").action( (s, tc) =>
        tc.copy(seconds = Some(s)) ).text("elapsed seconds")
    )
    cmd("get").action((_,tc) => tc.copy(cmd = "get")).text("get timer").children(
      opt[Int]('i',"timerId").required().action((i,tc) =>
        tc.copy(id = i)).text("id of timer")
    )
    cmd("elapsed").action((_,tc) => tc.copy(cmd = "elapsed")).text("get elapsed time for timer").
      children(
        opt[String]("format").abbr("f").action( (c, tc) =>
          tc.copy(fmt = c) ).text("format d (days), s (seconds) f (full)"),
        opt[Int]('i',"timerId").required().action((i,tc) =>
          tc.copy(id = i)).text("id of timer")
      )
    cmd("remaining").action((_,tc) => tc.copy(cmd = "remaining")).text("get remaining time for timer").
    children(
      opt[String]("format").abbr("f").action( (c, tc) =>
        tc.copy(fmt = c) ).text("format d (days), s (seconds) f (full)"),
      opt[Int]('i',"timerId").required().action((i,tc) =>
        tc.copy(id = i)).text("id of timer")
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
          val a = AddTimer(title = config.title.get, seconds = config.seconds)
          val t = Await.result(c.add(a), 2 seconds)
          println(t)
        }
        case "elapsed" => {
          val id = config.id
          val counter: Counter = config.fmt match {
            case "s" => Await.result(c.elapsedSeconds(id), 2 seconds)
            case "d" => Await.result(c.elapsedDays(id), 2 seconds)
            case _ => Await.result(c.elapsed(id), 2 seconds)
          }
          println(counter)
        }
        case "remaining" => {
          val id = config.id
          val counter: Counter = config.fmt match {
            case "s" => Await.result(c.remainingSeconds(id), 2 seconds)
            case "d" => Await.result(c.remainingDays(id), 2 seconds)
            case _ => Await.result(c.remaining(id), 2 seconds)
          }
          println(counter)
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
