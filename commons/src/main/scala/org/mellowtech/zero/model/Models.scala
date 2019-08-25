package org.mellowtech.zero.model

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId, ZoneOffset}
import java.util.UUID
import org.mellowtech.zero.grpc.{ZCounter, ZCounterType, ZTimer}



case class Timer(id: Option[UUID],
                 title: String,
                 start: OffsetDateTime,
                 stop: OffsetDateTime,
                 description: Option[String]) {

  override def toString: String = {
    s"id: ${id.getOrElse("")}\ntitle: $title\nstart: $start\nstop: $stop\ndescription: ${description.getOrElse("")}"
  }
}

case class AddTimer(title: String,
                    start: Option[OffsetDateTime] = None,
                    stop: Option[OffsetDateTime] = None,
                    millis: Option[Long] = None,
                    desc: Option[String] = None)

case class Counter(years: Option[Long] = None, months: Option[Long] = None,
                   days: Option[Long] = None, hours: Option[Long] = None,
                   minutes: Option[Long] = None, seconds: Option[Long] = None, millis: Option[Long] = None) {

  override def toString: String = {
    val sbuilder = new StringBuilder
    if(years.isDefined) sbuilder ++= s"years: ${years.get}\n"
    if(months.isDefined) sbuilder ++= s"months: ${months.get}\n"
    if(days.isDefined) sbuilder ++= s"days: ${days.get}\n"
    if(hours.isDefined) sbuilder ++= s"hours: ${hours.get}\n"
    if(minutes.isDefined) sbuilder ++= s"minutes: ${minutes.get}\n"
    if(seconds.isDefined) sbuilder ++= s"seconds: ${seconds.get}\n"
    if(millis.isDefined) sbuilder ++= s"millis: ${millis.get}\n"
    sbuilder.dropRight(1).result
  }
}

object SuccessFailure {
  val SUCCESS = "success"
  val ERROR = "error"
}

/*
sealed trait ApiResponse[T]{
  def status: String
  def message: Option[String]
  def value: Option[T]
}
*/


/*
case class TimersResponse(status: String = SuccessFailure.SUCCESS,
                         message: Option[String] = None,
                         value: Option[Seq[Timer]]) extends ApiResponse[Seq[Timer]]

case class TimerResponse(status: String = SuccessFailure.SUCCESS,
                          message: Option[String] = None,
                          value: Option[Timer]) extends ApiResponse[Timer]

case class CounterResponse(status: String = SuccessFailure.SUCCESS,
                         message: Option[String] = None,
                         value: Option[Counter]) extends ApiResponse[Counter]
*/

/*
object JsonCodecs {
  implicit val timerCodec: JsonValueCodec[Timer] = JsonCodecMaker.make[Timer](CodecMakerConfig())
  implicit val addTimerCodec: JsonValueCodec[AddTimer] = JsonCodecMaker.make[AddTimer](CodecMakerConfig())
  implicit val counterCodec: JsonValueCodec[Counter] = JsonCodecMaker.make[Counter](CodecMakerConfig())
  implicit val timerRepsonseCodec: JsonValueCodec[TimerResponse] = JsonCodecMaker.make[TimerResponse](CodecMakerConfig())
  implicit val timersRepsonseCodec: JsonValueCodec[TimersResponse] = JsonCodecMaker.make[TimersResponse](CodecMakerConfig())
  implicit val counterRepsonseCodec: JsonValueCodec[CounterResponse] = JsonCodecMaker.make[CounterResponse](CodecMakerConfig())
}
 */

object GrpcConverts {

  def toTimer(zt: ZTimer): Timer = {
    val uuid = if(zt.uuid != "") Some(UUID.fromString(zt.uuid)) else None
    val start = OffsetDateTime.parse(zt.start)
    val stop = OffsetDateTime.parse(zt.stop)
    val offset = start.getOffset
    val description = if(zt.desc == "") None else Some(zt.desc)
    Timer(uuid, zt.title, start, stop, description)
  }

  def toCounter(zc: ZCounter, ct: ZCounterType): Counter = ct match {
    case ZCounterType.MILLIS => Counter(millis = Some(zc.millis))
    case ZCounterType.SECONDS => Counter(
      seconds = Some(zc.seconds),
      millis = Some(zc.millis)
    )
    case ZCounterType.MINUTES => Counter(
      minutes = Some(zc.minutes),
      seconds = Some(zc.seconds),
      millis = Some(zc.millis)
    )
    case ZCounterType.HOURS => Counter(
      hours = Some(zc.hours),
      minutes = Some(zc.minutes),
      seconds = Some(zc.seconds),
      millis = Some(zc.millis)
    )
    case ZCounterType.DAYS => Counter(
      days = Some(zc.days),
      hours = Some(zc.hours),
      minutes = Some(zc.minutes),
      seconds = Some(zc.seconds),
      millis = Some(zc.millis)
    )
    case ZCounterType.MONTHS => Counter(
      months = Some(zc.months),
      days = Some(zc.days),
      hours = Some(zc.hours),
      minutes = Some(zc.minutes),
      seconds = Some(zc.seconds),
      millis = Some(zc.millis)
    )
    case _ => Counter(
      years = Some(zc.years),
      months = Some(zc.months),
      days = Some(zc.days),
      hours = Some(zc.hours),
      minutes = Some(zc.minutes),
      seconds = Some(zc.seconds),
      millis = Some(zc.millis)
    )
  }

  def toZTimer(t: Option[Timer]): ZTimer = t match {
    case Some(x) => toZTimer(x)
    case None => ZTimer()
  }

  def toZTimer(t: Timer): ZTimer = {
    val uuid = if(t.id.isDefined) t.id.get.toString else ""
    val desc = t.description.getOrElse("")
    ZTimer(uuid, t.title, t.start.toString, t.stop.toString, desc)
  }
}