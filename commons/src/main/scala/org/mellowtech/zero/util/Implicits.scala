package org.mellowtech.zero.util

import java.time.OffsetDateTime

import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JNull, JString}


case object OffsetDateTimeSerializer extends CustomSerializer[OffsetDateTime](format => (
  {
    case JString(t) => OffsetDateTime.parse(t)
    case JNull => null
  },
  {
    case t: OffsetDateTime => JString(t.toString)
  }
))

object Implicits {
  implicit val formats = org.json4s.DefaultFormats + OffsetDateTimeSerializer
}
