package org.mellowtech.zero.server

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

class DbService() extends Config{

  val driver = slick.jdbc.PostgresProfile
  import driver.api._
  val db = Database.forConfig("db")

}