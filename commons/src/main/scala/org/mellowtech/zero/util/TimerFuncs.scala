package org.mellowtech.zero.util

import java.time.{Instant, OffsetDateTime, ZoneId, ZoneOffset}
import java.time.temporal.ChronoUnit

import org.mellowtech.zero.grpc.{ZCounter, ZCounterType}
import org.mellowtech.zero.model.{Counter, Timer}

/**
  * @author msvens
  * @since 04/10/16
  */
object TimerFuncs {

  /*
  def utcNow: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
  def split(odt: OffsetDateTime): (Instant,ZoneId) = (odt.toInstant(), odt.getOffset)
  def merge(i: Instant, z: ZoneId): OffsetDateTime = OffsetDateTime.ofInstant(i, z)
  */

  val yearUnits: List[ChronoUnit] = List(ChronoUnit.YEARS,ChronoUnit.MONTHS,ChronoUnit.DAYS,ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS, ChronoUnit.MILLIS)
  val monthUnits:  List[ChronoUnit] = List(ChronoUnit.MONTHS, ChronoUnit.DAYS,ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS, ChronoUnit.MILLIS)
  val dayUnits: List[ChronoUnit] = List(ChronoUnit.DAYS,ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS, ChronoUnit.MILLIS)
  val hourUnits: List[ChronoUnit] = List(ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS, ChronoUnit.MILLIS)
  val minuteUnits: List[ChronoUnit] = List(ChronoUnit.MINUTES,ChronoUnit.SECONDS, ChronoUnit.MILLIS)
  val secondUnits: List[ChronoUnit] = List(ChronoUnit.SECONDS, ChronoUnit.MILLIS)
  val milliUnits: List[ChronoUnit] = List(ChronoUnit.MILLIS)

  def toUnits(counterType: ZCounterType): List[ChronoUnit]  = counterType match {
    case ZCounterType.YEARS => yearUnits
    case ZCounterType.MONTHS => monthUnits
    case ZCounterType.DAYS => dayUnits
    case ZCounterType.HOURS => hourUnits
    case ZCounterType.MINUTES => minuteUnits
    case ZCounterType.SECONDS => secondUnits
    case ZCounterType.MILLIS => milliUnits
    case _ => yearUnits
  }

  def counter(t: Timer, units: List[ChronoUnit], remaining: Boolean = false): Counter = remaining match {
    case false => elapsedTime(t, units)
    case true => remainingTime(t, units)
  }

  def elapsedTime(t: Timer, units: List[ChronoUnit]): Counter = {
    counterCalc(t.start, Instant.now(), units)
  }

  def remainingTime(t: Timer, units: List[ChronoUnit]): Counter = {
    counterCalc(Instant.now(), t.stop, units)
  }

  def counterCalc(from: Instant, to: Instant, units: List[ChronoUnit]): Counter = {
    require(from.isBefore(to), "From is After To")
    require(units != Nil)
    val ca = calcExtendedDuration(from.atOffset(ZoneOffset.UTC), to.atOffset(ZoneOffset.UTC), units)
    Counter(years = ca.get(ChronoUnit.YEARS),
      months = ca.get(ChronoUnit.MONTHS),
      days = ca.get(ChronoUnit.DAYS),
      hours = ca.get(ChronoUnit.HOURS),
      minutes = ca.get(ChronoUnit.MINUTES),
      seconds = ca.get(ChronoUnit.SECONDS),
      millis = ca.get(ChronoUnit.MILLIS))
  }


  /**
   * Needs to be on OffsetDateTime since instant cant handle units larger than days since that depends
   * on the time offset
   */
  def calcExtendedDuration(from: OffsetDateTime, to: OffsetDateTime, units: List[ChronoUnit]): Map[ChronoUnit,Long] =  units match{
    case Nil => Map()
    case h :: tail => {
      val amount = from.until(to, h)
      calcExtendedDuration(from.plus(amount,h),to,tail) + ((h,amount))
    }
  }


  /*
  def calcExtendedDuration(from: Instant, to: Instant, units: List[ChronoUnit]): Map[ChronoUnit,Long] =  units match{
    case Nil => Map()
    case h :: tail => {
      val amount = from.until(to, h)
      calcExtendedDuration(from.plus(amount,h),to,tail) + ((h,amount))
    }
  }
   */

}
