package org.mellowtech.zero.server

/**
  * @author msvens
  * @since 01/10/16
  */
import com.typesafe.config.ConfigFactory

trait Config {
  private val config = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")
  //private val databaseConfig = config.getConfig("database")

  //val dbConfig = config.getConfig("db")
  val httpHost = httpConfig.getString("interface")
  val httpPort = httpConfig.getInt("port")

  /*val jdbcUrl = databaseConfig.getString("url")
  val dbUser = databaseConfig.getString("user")
  val dbPassword = databaseConfig.getString("password")*/
}