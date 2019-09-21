package org.mellowtech.zero.model

import java.time.{Instant, ZoneId}
import java.util.UUID

import org.mellowtech.zero.grpc.{ZCounter, ZCounterType, ZInstant, ZNewTimer, ZSplit, ZTimer}

case class Split(id: Long, timer: Long, time: Instant, description: Option[String] = None)

case class User(id: Long, username: String, email: String)

case class Timer(id: Long,
                 user: Option[Long],
                 title: String,
                 start: Instant,
                 stop: Instant,
                 zone: ZoneId,
                 description: Option[String] = None) {

  override def toString: String = {
    s"id: ${id}\ntitle: $title\nstart: $start\nstop: $stop\ndescription: ${description.getOrElse("")}"
  }
}

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

object GrpcConverts {

  def toOption(a: Byte): Option[Byte] = if(a == 0) None else Some(a)
  def toOption(a: Short): Option[Short] = if(a == 0) None else Some(a)
  def toOption(a: Int): Option[Int] = if(a == 0) None else Some(a)
  def toOption(a: Long): Option[Long] = if(a == 0) None else Some(a)
  def toOption(a: Float): Option[Float] = if(a == 0.0) None else Some(a)
  def toOption(a: Double): Option[Double] = if(a == 0.0) None else Some(a)
  def toOption(a: String): Option[String] = if(a == "") None else Some(a)
  def toOption(a: Boolean): Option[Boolean] = if(a == false) None else Some(a)
  def toOption(a: Array[Byte]): Option[Array[Byte]] = if(a == null || a.isEmpty) None else Some(a)


  def fromOption(a: Option[Int]): Int = a.getOrElse(0)
  def fromOption(a: Option[Short]): Short = a.getOrElse(0)
  def fromOption(a: Option[Byte]): Byte = a.getOrElse(0)
  def fromOption(a: Option[Long]): Long = a.getOrElse(0)
  def fromOption(a: Option[Float]): Float = a.getOrElse(0)
  def fromOption(a: Option[Double]): Double = a.getOrElse(0)
  def fromOption(a: Option[String]): String = a.getOrElse("")
  def fromOption(a: Option[Boolean]): Boolean = a.getOrElse(false)
  def fromOption(a: Option[Array[Byte]]): Array[Byte] = a.getOrElse(Array())


  def toSplit(zs: ZSplit): Split = {
    val instant = zs.time.isDefined match {
      case true => toInstant(zs.time.get)
      case false => Instant.now()
    }
    Split(zs.id, zs.timer, instant, toOption(zs.description))
  }

  def toZSplit(s: Split): ZSplit = ZSplit(
    id = s.id, timer = s.timer,
    time = Some(toZInstant(s.time)), description = fromOption(s.description)
  )


  def toInstant(zi: ZInstant): Instant = Instant.ofEpochSecond(zi.seconds, zi.nanos)

  def toZInstant(i: Instant): ZInstant = ZInstant(i.getEpochSecond, i.getNano)

  def toTimer(zt: ZTimer): Timer = toTimer(zt, None)
  def toTimer(zt: ZTimer, user: Option[Long]): Timer = Timer(zt.id, user, zt.title, toInstant(zt.start.get), toInstant(zt.stop.get),
    ZoneId.of(zt.zone), toOption(zt.desc))


  def toZCounter(c: Counter): ZCounter = {
    ZCounter(
      years = c.years.getOrElse(0),
      months = c.months.getOrElse(0),
      days = c.days.getOrElse(0),
      hours = c.hours.getOrElse(0),
      minutes = c.minutes.getOrElse(0),
      seconds = c.seconds.getOrElse(0),
      millis = c.millis.getOrElse(0))
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

  def parseDuration(z: ZNewTimer): Either[Instant, Long] = {
    if(z.duration.isEmpty)
      Right(1000*3600*24)
    else if(z.duration.isMillis)
      Right(z.duration.millis.get)
    else
      Left(toInstant(z.duration.stop.get))
  }

  def parseZoneId(z: ZNewTimer): ZoneId = z.zoneId match {
    case "" => ZoneId.of("UTC")
    case _ => ZoneId.of(z.zoneId)
  }

  def parseStart(z: ZNewTimer): Instant = {
    if (z.start.isDefined) toInstant(z.start.get)
    else Instant.now()
  }

  def toZTimer(t: Option[Timer]): ZTimer = t match {
    case Some(x) => toZTimer(x)
    case None => ZTimer()
  }

  def toZTimer(t: Timer): ZTimer = {
    val desc = t.description.getOrElse("")
    ZTimer(t.id, t.title, Some(toZInstant(t.start)), Some(toZInstant(t.stop)),
      t.zone.getId, desc)
  }
}