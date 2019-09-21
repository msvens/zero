package org.mellowtech.zero.client
import java.time.Instant
import java.util.regex.Pattern
import org.rogach.scallop.exceptions.{Help, ScallopException, ScallopResult, Version}
import org.rogach.scallop.{ScallopConf, Subcommand, throwError}

import scala.concurrent.Future


class ShellConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  version("0.1.0 2019 Mellowtech")

  val add = new Subcommand("add"){
    val title = opt[String](required = true, name = "title", descr = "Name your timer")
    val millis = opt[Long](name = "millies", descr = "Milliseconds from now")
  }

  val get = new Subcommand("get"){
    val id = opt[Long]("id", descr = "timer id")
    val name = opt[String]("name", descr = "timer title")
    val index = opt[Int](name = "index", descr = "list index")
    requireOne(id, name, index)
  }

  val elapsed = new Subcommand("elapsed"){
    val format = opt[String](name = "format", descr = "format f (full) mo (months) d (days) h (hours) m (minutes) s (seconds) mi (millis)")
    val id = opt[Long]("id", descr = "timer id")
    val name = opt[String]("name", descr = "timer title")
    val index = opt[Int](name = "index", descr = "list index")
    requireOne(id, name, index)
  }

  val remaining = new Subcommand("remaining"){
    val format = opt[String](name = "format", descr = "format f (full) mo (months) d (days) h (hours) m (minutes) s (seconds) mi (millis)")
    val id = opt[Long]("id", descr = "timer id")
    val name = opt[String]("name", descr = "timer title")
    val index = opt[Int](name = "index", descr = "list index")
    requireOne(id, name, index)
  }

  val list = new Subcommand("list")

  val set = new Subcommand("set"){
    val url = opt[String]("url")
  }


  val exit = new Subcommand("exit")

  //Some better help text
  elapsed.descr("Get the elapased time since start of timer")
  remaining.descr("Get the remaing time until timer stops")
  get.descr("Retrieve a specific timer")
  add.descr("Add timer")
  list.descr("List available timers")
  set.descr("Set new url to query")

  addSubcommand(add)
  addSubcommand(elapsed)
  addSubcommand(remaining)
  addSubcommand(get)
  addSubcommand(list)
  addSubcommand(set)
  addSubcommand(exit)

  errorMessageHandler = (m: String) => {
    verified = false
    println("Could not verify input: "+m)
    printHelp()
  }

  //Override default onError to not exit
  override protected def onError(e: Throwable): Unit = e match {
    case r: ScallopResult if !throwError.value => r match {
      case Help("") =>
        builder.printHelp
      //sys.exit(0)
      case Help(subname) =>
        builder.findSubbuilder(subname).get.printHelp
        verified = false
      //sys.exit(0)
      case Version =>
        builder.vers.foreach(println)
      //sys.exit(0)
      case ScallopException(message) => errorMessageHandler(message)
    }
    case e => {
      Console.println("throwing error")
      throw e
    }
  }


}


class ZeroShell(c: ZClient) {

  import scala.concurrent.Await
  import scala.util.{Failure, Success}
  import scala.concurrent.duration._


  var currentUrl: Option[String] = None


  def sync[A](f: Future[A]): A = {
    Await.result(f, 2.seconds)
  }

  def ready[A](f: Future[A]): Unit = {
    val r = Await.ready(f, 2.seconds).value.get
    r match {
      case Success(a) => println(a)
      case Failure(e) => println(e)
    }
  }

  //def printCounter(c: Counter)



  def exec(conf: ShellConf): Unit = {
    conf.subcommand match {
      case None => conf.printHelp()
      case Some(cmd) => cmd match {
        case conf.add => {
          val t = conf.add.title()
          val m = conf.add.millis.getOrElse(1000*3600*24)
          val duration: Either[Instant, Long] = Right(m)
          val n = Instant.now()
          val timer = sync(c.addTimer(t,Instant.now(),duration))
          println(timer)
        }
        case conf.get => {
          val id = conf.get.id
          val title = conf.get.name
          val index = conf.get.index
          if(id.isDefined){
            val timer = sync(c.getTimer(id()))
            //val timer = sync(c.getTimer(UUID.fromString(uuid())))
            println(timer)
          } else
            println("currently not implemented")
        }
        case conf.exit => {
          val result = Await.result(c.close, 5.seconds)
          println(result)
          sys.exit(0)
        } //should signal in a better way...i.e. return a flag
        case conf.set => {
          //if(conf.set.url.isDefined) c.url = conf.set.url()
          Console.println("currently disabled")
        }
        case conf.elapsed => {
          if(conf.elapsed.id.isDefined){
            val result = sync(c.counter(conf.elapsed.id(), false))
            println(result)
          } else
            Console.println("currently disabled")
        }
        case conf.remaining => {
          if(conf.remaining.id.isDefined){
            ready(c.counter(conf.remaining.id(), true))
            //println(result)
          } else
            Console.println("currently disabled")
        }
        case conf.list => {
          val tl = Await.ready(c.list, 2.seconds).value.get
          tl match {
            case Success(l) => {
              val m = (l.indices zip l).toMap
              for((k,v) <- m) Console.println(s"$k\t$v")
            }
            case Failure(f) => println(f)
          }
        }
      }
    }
  }
}

object ZeroApp extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val c = new ZClient("localhost", 9090)
  val shell = new ZeroShell(c)

  val regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")

  while(true){
    Console.print("zero> ")
    val cmd = stringSplitter(scala.io.StdIn.readLine())
    val conf = new ShellConf(cmd)
    try {
      conf.verify()
    } catch {case e:ScallopException => conf.printHelp()}
    if(conf.verified)
      shell.exec(conf)
  }
  throw new Exception("should not happen")

  def stringSplitter(str: String): Seq[String] = {
    var ret: Seq[String] = Seq()
    val matcher = regex.matcher(str)
    while(matcher.find()){
      if(matcher.group(1) != null) {
        ret = ret :+ matcher.group(1)
      }
      else if(matcher.group(2) != null) {
        ret = ret :+ matcher.group(2)
      }
      else {
        ret = ret :+ matcher.group()
      }
    }
    ret
  }

}
