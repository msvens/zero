package org.mellowtech.zero.util

import org.mellowtech.jsonclient.JavaTimeSerializers


object Implicits {
  implicit val formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all
}
