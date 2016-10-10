package org.mellowtech.zero.model

import java.time.OffsetDateTime

import org.json4s.JValue


case class Timer(id: Option[Int],
                 title: String,
                 start: Option[OffsetDateTime],
                 stop: Option[OffsetDateTime],
                 desc: Option[String])

case class AddTimer(title: String,
                    start: Option[OffsetDateTime] = None,
                    stop: Option[OffsetDateTime] = None,
                    seconds: Option[Int] = None,
                    desc: Option[String] = None)

case class Counter(years: Option[Long] = None, months: Option[Long] = None,
                   days: Option[Long] = None, hours: Option[Long] = None,
                   minutes: Option[Long] = None, seconds: Option[Long] = None)

object SuccessFailure {
  val SUCCESS = "success"
  val ERROR = "error"
}


case class ApiResponse[T](status: String = SuccessFailure.SUCCESS, message: Option[String] = None, value: Option[T] = None)