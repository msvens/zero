package org.mellowtech.zero.client

import org.mellowtech.jsonclient.JsonClient
import org.mellowtech.zero.model.{AddTimer, ApiResponse, Counter, Timer}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author msvens
  * @since 01/10/16
  */
class Client(url: String)(implicit val ec: ExecutionContext) {

  import org.mellowtech.zero.util.Implicits.formats
  val jc = JsonClient()

  def list: Future[List[Timer]] = for(
    jcresp <- jc.get[ApiResponse[List[Timer]]](url+"/timers")
    if jcresp.status == 200
  ) yield jcresp.body.get.value.get

  def add(ad: AddTimer): Future[Timer] = for(
    r <- jc.post[ApiResponse[Timer],AddTimer](url+"/timers",ad)
    if r.status == 200
  ) yield r.body.get.value.get

  def get(id: Int): Future[Timer] = for(
    r <- jc.get[ApiResponse[Timer]](url+"/timers/"+id)
    if r.status == 200
  ) yield r.body.get.value.get

  def elapsed(id: Int): Future[Counter] = for (
    r <- jc.get[ApiResponse[Counter]](url+"/timers/"+id+"/elapsed")
    if r.status == 200
  ) yield r.body.get.value.get

  def elapsedDays(id: Int): Future[Counter] = for (
    r <- jc.get[ApiResponse[Counter]](url+"/timers/"+id+"/elapsed/days")
    if r.status == 200
  ) yield r.body.get.value.get

  def elapsedSeconds(id: Int): Future[Counter] = for (
    r <- jc.get[ApiResponse[Counter]](url+"/timers/"+id+"/elapsed/seconds")
    if r.status == 200
  ) yield r.body.get.value.get

  def remaining(id: Int): Future[Counter] = for (
    r <- jc.get[ApiResponse[Counter]](url+"/timers/"+id+"/remaining")
    if r.status == 200
  ) yield r.body.get.value.get

  def remainingDays(id: Int): Future[Counter] = for (
    r <- jc.get[ApiResponse[Counter]](url+"/timers/"+id+"/remaining/days")
    if r.status == 200
  ) yield r.body.get.value.get

  def remainingSeconds(id: Int): Future[Counter] = for (
    r <- jc.get[ApiResponse[Counter]](url+"/timers/"+id+"/remaining/seconds")
    if r.status == 200
  ) yield r.body.get.value.get





  def close: Unit = {
    jc.close
  }
}

object Client {

  def apply(url: String)(implicit ec: ExecutionContext): Client = {
    new Client(url)
  }


}
