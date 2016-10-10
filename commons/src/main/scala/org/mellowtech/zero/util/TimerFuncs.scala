package org.mellowtech.zero.util

import java.time.{OffsetDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit

import org.mellowtech.zero.model.{Counter, Timer}

/**
  * @author msvens
  * @since 04/10/16
  */
object TimerFuncs {

  def utcNow: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
  def allUnits: List[ChronoUnit] = List(ChronoUnit.YEARS,ChronoUnit.MONTHS,ChronoUnit.DAYS,ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS)
  def toDayUnits: List[ChronoUnit] = List(ChronoUnit.DAYS,ChronoUnit.HOURS,ChronoUnit.MINUTES,ChronoUnit.SECONDS)

  def elapsedSeconds(t: Timer): Counter = elapsed(t, List(ChronoUnit.SECONDS))
  def elapsedToDays(t: Timer): Counter = elapsed(t, toDayUnits)
  def elapsedFull(t: Timer): Counter = elapsed(t, allUnits)
  def elapsed(t: Timer, units: List[ChronoUnit]): Counter = {
    counter(t.start.get, utcNow, units)
  }

  def remainingSeconds(t: Timer): Counter = remaining(t, List(ChronoUnit.SECONDS))
  def remainingToDays(t: Timer): Counter = remaining(t, toDayUnits)
  def remainingFull(t: Timer): Counter = remaining(t, allUnits)
  def remaining(t: Timer, units: List[ChronoUnit]): Counter = {
    counter(utcNow, t.stop.get, units)
  }

  def counter(from: OffsetDateTime, to: OffsetDateTime, units: List[ChronoUnit]): Counter = {
    require(from.isBefore(to))
    val ca = calcExtendedDuration(from, to, units)
    Counter(years = ca.get(ChronoUnit.YEARS), months = ca.get(ChronoUnit.MONTHS), days = ca.get(ChronoUnit.DAYS),
      hours = ca.get(ChronoUnit.HOURS), minutes = ca.get(ChronoUnit.MINUTES), seconds = ca.get(ChronoUnit.SECONDS))
  }

  def calcExtendedDuration(from: OffsetDateTime, to: OffsetDateTime, units: List[ChronoUnit]): Map[ChronoUnit,Long] =  units match{
    case Nil => Map()
    case h :: tail => {
      val amount = from.until(to, h)
      calcExtendedDuration(from.plus(amount,h),to,tail) + ((h,amount))
    }
  }

}
