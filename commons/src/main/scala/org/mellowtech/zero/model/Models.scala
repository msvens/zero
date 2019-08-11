package org.mellowtech.zero.model

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId, ZoneOffset}
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import org.mellowtech.zero.grpc.ZTimer



case class Timer(id: Option[UUID],
                 title: String,
                 start: OffsetDateTime,
                 stop: OffsetDateTime,
                 description: Option[String])

case class AddTimer(title: String,
                    start: Option[OffsetDateTime] = None,
                    stop: Option[OffsetDateTime] = None,
                    millis: Option[Long] = None,
                    desc: Option[String] = None)

case class Counter(years: Option[Long] = None, months: Option[Long] = None,
                   days: Option[Long] = None, hours: Option[Long] = None,
                   minutes: Option[Long] = None, seconds: Option[Long] = None)

object SuccessFailure {
  val SUCCESS = "success"
  val ERROR = "error"
}

sealed trait ApiResponse[T]{
  def status: String
  def message: Option[String]
  def value: Option[T]
}

case class TimersResponse(status: String = SuccessFailure.SUCCESS,
                         message: Option[String] = None,
                         value: Option[Seq[Timer]]) extends ApiResponse[Seq[Timer]]

case class TimerResponse(status: String = SuccessFailure.SUCCESS,
                          message: Option[String] = None,
                          value: Option[Timer]) extends ApiResponse[Timer]

case class CounterResponse(status: String = SuccessFailure.SUCCESS,
                         message: Option[String] = None,
                         value: Option[Counter]) extends ApiResponse[Counter]


object JsonCodecs {
  implicit val timerCodec: JsonValueCodec[Timer] = JsonCodecMaker.make[Timer](CodecMakerConfig())
  implicit val addTimerCodec: JsonValueCodec[AddTimer] = JsonCodecMaker.make[AddTimer](CodecMakerConfig())
  implicit val counterCodec: JsonValueCodec[Counter] = JsonCodecMaker.make[Counter](CodecMakerConfig())
  implicit val timerRepsonseCodec: JsonValueCodec[TimerResponse] = JsonCodecMaker.make[TimerResponse](CodecMakerConfig())
  implicit val timersRepsonseCodec: JsonValueCodec[TimersResponse] = JsonCodecMaker.make[TimersResponse](CodecMakerConfig())
  implicit val counterRepsonseCodec: JsonValueCodec[CounterResponse] = JsonCodecMaker.make[CounterResponse](CodecMakerConfig())
}

object GrpcConverts {

  def toTimer(zt: ZTimer): Timer = {
    val uuid = if(zt.uuid != "") Some(UUID.fromString(zt.uuid)) else None
    val start = OffsetDateTime.parse(zt.start)
    val stop = OffsetDateTime.parse(zt.stop)
    val offset = start.getOffset
    val description = if(zt.desc == "") None else Some(zt.desc)
    Timer(uuid, zt.title, start, stop, description)
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