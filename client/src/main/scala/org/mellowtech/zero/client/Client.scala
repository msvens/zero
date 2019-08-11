package org.mellowtech.zero.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mellowtech.jsonclient.JsonClient
import org.mellowtech.zero.model.{AddTimer, Counter, CounterResponse, Timer, TimerResponse, TimersResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author msvens
  * @since 01/10/16
  */
class Client(var url: String)(implicit val ec: ExecutionContext) {

  import org.mellowtech.zero.model.JsonCodecs._

  implicit val as: ActorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val jc = JsonClient()


  def list: Future[Seq[Timer]] = for(
    r <- jc.get[TimersResponse](url+"/timers")
    if r.status == 200
  ) yield r.body.value.get

  def add(ad: AddTimer): Future[Timer] = for(
    r <- jc.post[TimerResponse,AddTimer](url+"/timers",ad)
    if r.status == 200
  ) yield r.body.value.get

  def get(id: Int): Future[Timer] = for(
    r <- jc.get[TimerResponse](url+"/timers/"+id)
    if r.status == 200
  ) yield r.body.value.get

  def elapsed(id: Int): Future[Counter] = for (
    r <- jc.get[CounterResponse](url+"/timers/"+id+"/elapsed")
    if r.status == 200
  ) yield r.body.value.get

  def elapsedDays(id: Int): Future[Counter] = for (
    r <- jc.get[CounterResponse](url+"/timers/"+id+"/elapsed/days")
    if r.status == 200
  ) yield r.body.value.get

  def elapsedSeconds(id: Int): Future[Counter] = for (
    r <- jc.get[CounterResponse](url+"/timers/"+id+"/elapsed/seconds")
    if r.status == 200
  ) yield r.body.value.get

  def remaining(id: Int): Future[Counter] = for (
    r <- jc.get[CounterResponse](url+"/timers/"+id+"/remaining")
    if r.status == 200
  ) yield r.body.value.get

  def remainingDays(id: Int): Future[Counter] = for (
    r <- jc.get[CounterResponse](url+"/timers/"+id+"/remaining/days")
    if r.status == 200
  ) yield r.body.value.get

  def remainingSeconds(id: Int): Future[Counter] = for (
    r <- jc.get[CounterResponse](url+"/timers/"+id+"/remaining/seconds")
    if r.status == 200
  ) yield r.body.value.get





  def close: Unit = {
    jc.close
  }
}

object Client {

  def apply(url: String)(implicit ec: ExecutionContext): Client = {
    new Client(url)
  }


}
