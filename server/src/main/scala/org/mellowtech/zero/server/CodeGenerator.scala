package org.mellowtech.zero.server




object CodeGenerator extends App with Config{


  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.util.{Failure, Success}
  import slick.jdbc.PostgresProfile.api._
  import slick.jdbc.PostgresProfile


  /*
  slick.codegen.SourceCodeGenerator.main(
    Array("slick.jdbc.PostgresProfile", "org.postgresql.Driver",
      jdbcUrl, "server/src/main/scala", "org.mellowtech.zero.db", dbUser, dbPassword)
  )
  */



}
