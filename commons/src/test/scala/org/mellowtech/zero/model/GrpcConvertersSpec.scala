package org.mellowtech.zero.model

import java.time.{Instant, ZoneId, ZoneOffset}

import org.mellowtech.zero.grpc.{ZCounterType, ZInstant}
import org.scalatest.{FlatSpec, Matchers}

class GrpcConvertersSpec extends FlatSpec with Matchers{

  import GrpcConverts._

  val start = Instant.now()
  val stop = start.plusSeconds(3600)
  val zone = ZoneId.of("UTC")

  val timer = Timer(1L, None, "timer", start, stop, zone, Some("description"))

  val counter = Counter(Some(1), Some(1), None, Some(1), Some(1), Some(1), Some(1))
  val minuteCounter = Counter(None, None, None, None, Some(10), Some(10), Some(10))
  val split = Split(1, 1, start, Some("description"))
  val splitEmpty = Split(1, 1, start, None)

  def compareInstant(i: Instant, zi: ZInstant): Unit = {
    assert(i.getEpochSecond == zi.seconds)
    assert(i.getNano == zi.nanos)
  }

  behavior of "grpc converts"

  it should "convert a Split to ZSplit" in {
    val zs = toZSplit(split)
    assert(zs.description == "description")
    assert(zs.timer == 1)
    assert(zs.id == 1)
    compareInstant(start, zs.time.get)
  }

  it should "convert a Split with an empty description to ZSplit" in {
    val zs = toZSplit(splitEmpty)
    assert(zs.id == 1)
    assert(zs.timer == 1)
    compareInstant(start, zs.time.get)
    assert(zs.description == "")
  }

  it should "convert a ZSplit to Split" in {
    val s = toSplit(toZSplit(split))
    assert(s.id == split.id)
    assert(s.timer == split.timer)
    assert(s.time == split.time)
    assert(s.description.get == split.description.get)
  }

  it should "convert a ZSplit with an empty description to Split" in {
    val s = toSplit(toZSplit(splitEmpty))
    assert(s.id == splitEmpty.id)
    assert(s.timer == splitEmpty.timer)
    assert(s.time == splitEmpty.time)
    assert(s.description == splitEmpty.description)
  }

  it should "convert a Counter to ZCounter" in {
    val zc = toZCounter(counter)
    assert(zc.years == 1)
    assert(zc.months == 1)
    assert(zc.days == 0)
    assert(zc.hours == 1)
    assert(zc.minutes == 1)
    assert(zc.seconds == 1)
    assert(zc.millis == 1)
  }

  it should "convert a ZCounter to Counter" in {
    val c = toCounter(toZCounter(counter), ZCounterType.YEARS)
    assert(c.years.get == 1)
    assert(c.months.get == 1)
    assert(c.days.get == 0) //Observe when specifying the counter type Years all converted values will Some(xxx)
    assert(c.hours.get == 1)
    assert(c.minutes.get == 1)
    assert(c.seconds.get == 1)
    assert(c.millis.get == 1)
  }

  it should "convert a ZCounter minutes to Counter" in {
    val c = toCounter(toZCounter(minuteCounter), ZCounterType.MINUTES)
    assert(c.years == None)
    assert(c.months == None)
    assert(c.days == None)
    assert(c.hours == None)
    assert(c.minutes.get == 10)
    assert(c.seconds.get == 10)
    assert(c.millis.get == 10)

  }

  it should "convert an Instant to ZInstant" in {
    val zi = toZInstant(start)
    compareInstant(start, zi)
  }

  it should "convert a ZInstant to Instant" in {
    val i = toInstant(toZInstant(start))
    assert(start == i)
  }

  it should "convert a timer to ztimer" in {
    val zt = toZTimer(timer)
    assert(zt.id == timer.id)
    assert(zt.title == timer.title)
    compareInstant(start, zt.start.get)
    compareInstant(stop, zt.stop.get)
    assert("UTC" == zt.zone)
    assert("description" == zt.desc)
  }

  it should "convert a ztimer to timer" in {
    val t = toTimer(toZTimer(timer))
    assert(timer.id == t.id)
    assert(timer.title == t.title)
    assert(start == t.start)
    assert(stop == t.stop)
    assert(timer.zone == t.zone)
    assert(timer.description.get == t.description.get)

  }

  behavior of "fromOption"

  it should "convert Option[Number] None to 0" in {
    val byteZero: Option[Byte] = None
    val shortZero: Option[Short] = None
    val intZero: Option[Int] = None
    val longZero: Option[Long] = None
    val floatZero: Option[Float] = None
    val doubleZero: Option[Double] = None
    assert(fromOption(byteZero) == 0)
    assert(fromOption(shortZero) == 0)
    assert(fromOption(intZero) == 0)
    assert(fromOption(longZero) == 0)
    assert(fromOption(floatZero) == 0.0)
    assert(fromOption(doubleZero) == 0.0)
  }

  it should "convert Some(number) to number" in {
    val byteZero: Option[Byte] = Some(1)
    val shortZero: Option[Short] = Some(1)
    val intZero: Option[Int] = Some(1)
    val longZero: Option[Long] = Some(1)
    val floatZero: Option[Float] = Some(1)
    val doubleZero: Option[Double] = Some(1)
    assert(fromOption(byteZero) == 1)
    assert(fromOption(shortZero) == 1)
    assert(fromOption(intZero) == 1)
    assert(fromOption(longZero) == 1)
    assert(fromOption(floatZero) == 1.0)
    assert(fromOption(doubleZero) == 1.0)
  }

  it should "convert Option[Boolean] None to the empty false" in {
    val boolean: Option[Boolean] = None
    assert(fromOption(boolean) == false)
  }

  it should "convert Some(boolean) to boolean" in {
    val boolean: Option[Boolean] = Some(true)
    assert(fromOption(boolean) == true)
  }

  it should "convert Option[String] None to the empty string" in {
    val str: Option[String] = None
    assert(fromOption(str)== "")
  }

  it should "convert Some(tring) to string" in {
    val str: Option[String] = Some("string")
    assert(fromOption(str)== "string")
  }

  it should "convert Option[Array[Byte] None to the empty Array[Byte]" in {
    val arr: Option[Array[Byte]] = None
    assert(fromOption(arr).size == 0)
  }

  it should "convert Some(bytes) to bytes" in {
    val arr: Option[Array[Byte]] = Some("string".getBytes())
    assert(fromOption(arr).size == 6)
  }

  behavior of "toOption"

  it should "convert 0 to None (byte, short, int, long, float, double)" in {
    val byteZero: Byte = 0
    val shortZero: Short = 0
    val intZero: Int = 0
    val longZero: Long = 0
    val floatZero: Float = 0
    val doubleZero: Double = 0
    assert(toOption(byteZero) == None)
    assert(toOption(shortZero) == None)
    assert(toOption(intZero) == None)
    assert(toOption(longZero) == None)
    assert(toOption(floatZero) == None)
    assert(toOption(doubleZero) == None)
  }

  it should "convert the empty string to None" in {
    assert(toOption("") == None)
  }

  it should "convert false to None" in {
    assert(toOption(false) == None)
  }

  it should "convert an empty byte array to None" in {
    assert(toOption(Array[Byte]()) == None)
  }

  it should "convert a number not 0 to Some(number)" in {
    val byteZero: Byte = 1
    val shortZero: Short = 1
    val intZero: Int = 1
    val longZero: Long = 1
    val floatZero: Float = 1
    val doubleZero: Double = 1
    assert(toOption(byteZero).get == 1)
    assert(toOption(shortZero).get == 1)
    assert(toOption(intZero).get == 1)
    assert(toOption(longZero).get == 1)
    assert(toOption(floatZero).get == 1.0)
    assert(toOption(doubleZero).get == 1.0)
  }

  it should "convert true to Some(true)" in {
    assert(toOption(true).get == true)
  }

  it should "convert a non empty string to Some(string)" in {
    assert(toOption("some").get == "some")
  }

  it should "convert a non empty byte array to Some(bytes)" in {
    val bytes = "some".getBytes
    assert(toOption(bytes).get.size == 4)
  }


}
