package org.mellowtech.zero.util

import java.time.{Instant, OffsetDateTime, ZoneId, ZoneOffset}
import java.time.temporal.ChronoUnit

import org.mellowtech.zero.grpc.{ZCounter, ZCounters}
import org.mellowtech.zero.model.{Counter, Timer}

/**
  * @author msvens
  * @since 04/10/16
  */
object TimerFuncs {

  def utcNow: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
  def split(odt: OffsetDateTime): (Instant,ZoneId) = (odt.toInstant(), odt.getOffset)
  def merge(i: Instant, z: ZoneId): OffsetDateTime = OffsetDateTime.ofInstant(i, z)


  val yearUnits: List[ChronoUnit] = List(ChronoUnit.YEARS,ChronoUnit.MONTHS,ChronoUnit.DAYS,ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS)
  val monthUnits:  List[ChronoUnit] = List(ChronoUnit.MONTHS, ChronoUnit.DAYS,ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS)
  val dayUnits: List[ChronoUnit] = List(ChronoUnit.DAYS,ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS)
  val hourUnits: List[ChronoUnit] = List(ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS)
  val minuteUnits: List[ChronoUnit] = List(ChronoUnit.MINUTES,ChronoUnit.SECONDS)
  val secondUnits: List[ChronoUnit] = List(ChronoUnit.SECONDS)
  val milliUnits: List[ChronoUnit] = List(ChronoUnit.MILLIS)

  def toUnits(counters: ZCounters): List[ChronoUnit]  = counters match {
    case ZCounters.YEARS => yearUnits
    case ZCounters.MONTHS => monthUnits
    case ZCounters.DAYS => dayUnits
    case ZCounters.HOURS => hourUnits
    case ZCounters.MINUTES => minuteUnits
    case ZCounters.SECONDS => secondUnits
    case ZCounters.MILLIS => milliUnits
    case _ => yearUnits
  }

  def elapsedSeconds(t: Timer): Counter = elapsed(t, List(ChronoUnit.SECONDS))
  def elapsedToDays(t: Timer): Counter = elapsed(t, dayUnits)
  def elapsedFull(t: Timer): Counter = elapsed(t, yearUnits)
  def elapsed(t: Timer, units: List[ChronoUnit]): Counter = {
    counter(t.start, OffsetDateTime.now(t.stop.getOffset), units)
  }

  def remainingSeconds(t: Timer): Counter = remaining(t, List(ChronoUnit.SECONDS))
  def remainingToDays(t: Timer): Counter = remaining(t, dayUnits)
  def remainingFull(t: Timer): Counter = remaining(t, yearUnits)
  def remaining(t: Timer, units: List[ChronoUnit]): Counter = {
    counter(OffsetDateTime.now(t.start.getOffset), t.stop, units)
  }


  def counter(from: OffsetDateTime, to: OffsetDateTime, units: List[ChronoUnit]): Counter = {
    require(from.isBefore(to))
    val ca = calcExtendedDuration(from, to, units)
    Counter(years = ca.get(ChronoUnit.YEARS), months = ca.get(ChronoUnit.MONTHS), days = ca.get(ChronoUnit.DAYS),
      hours = ca.get(ChronoUnit.HOURS), minutes = ca.get(ChronoUnit.MINUTES), seconds = ca.get(ChronoUnit.SECONDS))
  }

  def zelapsed(t: Timer, units: List[ChronoUnit]): ZCounter = {
    zcounter(t.start, OffsetDateTime.now(t.stop.getOffset), units)
  }

  def zremaining(t: Timer, units: List[ChronoUnit]): ZCounter = {
    zcounter(OffsetDateTime.now(t.start.getOffset), t.stop, units)
  }

  def zcounter(from: OffsetDateTime, to: OffsetDateTime, units: List[ChronoUnit]): ZCounter = {
    require(from.isBefore(to))
    val ca = calcExtendedDuration(from, to, units)
    ZCounter(years = ca.getOrElse(ChronoUnit.YEARS, 0),
      months = ca.getOrElse(ChronoUnit.MONTHS, 0),
      days = ca.getOrElse(ChronoUnit.DAYS, 0),
      hours = ca.getOrElse(ChronoUnit.HOURS,0),
      minutes = ca.getOrElse(ChronoUnit.MINUTES, 0),
      seconds = ca.getOrElse(ChronoUnit.SECONDS,0))
  }




  def calcExtendedDuration(from: OffsetDateTime, to: OffsetDateTime, units: List[ChronoUnit]): Map[ChronoUnit,Long] =  units match{
    case Nil => Map()
    case h :: tail => {
      val amount = from.until(to, h)
      calcExtendedDuration(from.plus(amount,h),to,tail) + ((h,amount))
    }
  }

}
