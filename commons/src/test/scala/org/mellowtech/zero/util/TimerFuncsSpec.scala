package org.mellowtech.zero.util

import java.time.{Instant, ZoneOffset}
import java.time.temporal.ChronoUnit

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TimerFuncsSpec extends AnyFlatSpec with Matchers{

  import TimerFuncs._

  behavior of "counterCalc"

  it should "return the correct millis" in {
    val start = Instant.now
    val stop = start.plusMillis(1000)
    val units = milliUnits
    val counter = counterCalc(start, stop, units)
    assert(counter.millis.isDefined)
    assert(counter.millis.get == 1000)
  }

  it should "return the correct seconds and millis" in {
    val start = Instant.now
    val stop = start.plusMillis(999).plusSeconds(1)
    val units = secondUnits
    val counter = counterCalc(start, stop, units)
    assert(counter.seconds.isDefined)
    assert(counter.millis.get == 999)
    assert(counter.seconds.get == 1)
  }

  it should "return the correct years, months, days, hours, minutes, seconds, millis" in {
    val start = Instant.now
    val stop = start.atOffset(ZoneOffset.UTC).plusYears(1).plusMonths(1).
      plusDays(1).plusHours(1).
      plusMinutes(1).plusSeconds(1).plus(999, ChronoUnit.MILLIS).toInstant
    val units = yearUnits
    val counter = counterCalc(start, stop, units)
    assert(counter.years.isDefined)
    assert(counter.years.get == 1)
    assert(counter.months.get == 1)
    assert(counter.days.get == 1)
    assert(counter.hours.get == 1)
    assert(counter.minutes.get == 1)
    assert(counter.seconds.get == 1)
    assert(counter.millis.get == 999)
  }


  it should "fail if start is after stop" in {
    val now = Instant.now
    assertThrows[IllegalArgumentException](TimerFuncs.counterCalc(now, now.minusSeconds(1), Nil))
  }

  it should "fail if units are empty" in {
    val now = Instant.now
    assertThrows[IllegalArgumentException](TimerFuncs.counterCalc(now, now.plusSeconds(1), Nil))
  }

  behavior of "TimeUnitLists"

  it should "return millis" in {
    val millis = TimerFuncs.milliUnits
    assert(millis.size == 1)
    assert(millis.contains(ChronoUnit.MILLIS))
  }

  it should "return seconds and millis" in {
    val millis = TimerFuncs.secondUnits
    assert(millis.size == 2)
    assert(millis.contains(ChronoUnit.MILLIS))
    assert(millis.contains(ChronoUnit.SECONDS))
  }

  it should "return minutes, seconds and millis" in {
    val millis = TimerFuncs.minuteUnits
    assert(millis.size == 3)
    assert(millis.contains(ChronoUnit.MINUTES))
    assert(millis.contains(ChronoUnit.SECONDS))
    assert(millis.contains(ChronoUnit.MILLIS))
  }

  it should "return hours, minutes, seconds and millis" in {
    val millis = TimerFuncs.hourUnits
    assert(millis.size == 4)
    assert(millis.contains(ChronoUnit.HOURS))
    assert(millis.contains(ChronoUnit.MINUTES))
    assert(millis.contains(ChronoUnit.SECONDS))
    assert(millis.contains(ChronoUnit.MILLIS))
  }

  it should "return days, hours, minutes, seconds and millis" in {
    val millis = TimerFuncs.dayUnits
    assert(millis.size == 5)
    assert(millis.contains(ChronoUnit.DAYS))
    assert(millis.contains(ChronoUnit.HOURS))
    assert(millis.contains(ChronoUnit.MINUTES))
    assert(millis.contains(ChronoUnit.SECONDS))
    assert(millis.contains(ChronoUnit.MILLIS))
  }

  it should "return months, days, hours, minutes, seconds and millis" in {
    val millis = TimerFuncs.monthUnits
    assert(millis.size == 6)
    assert(millis.contains(ChronoUnit.MONTHS))
    assert(millis.contains(ChronoUnit.DAYS))
    assert(millis.contains(ChronoUnit.HOURS))
    assert(millis.contains(ChronoUnit.MINUTES))
    assert(millis.contains(ChronoUnit.SECONDS))
    assert(millis.contains(ChronoUnit.MILLIS))
  }

  it should "return years, months, days, hours, minutes, seconds and millis" in {
    val millis = TimerFuncs.yearUnits
    assert(millis.size == 7)
    assert(millis.contains(ChronoUnit.YEARS))
    assert(millis.contains(ChronoUnit.MONTHS))
    assert(millis.contains(ChronoUnit.DAYS))
    assert(millis.contains(ChronoUnit.HOURS))
    assert(millis.contains(ChronoUnit.MINUTES))
    assert(millis.contains(ChronoUnit.SECONDS))
    assert(millis.contains(ChronoUnit.MILLIS))
  }






}
