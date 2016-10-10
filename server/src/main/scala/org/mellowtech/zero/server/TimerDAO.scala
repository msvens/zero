package org.mellowtech.zero.server

import java.sql.Timestamp
import java.time.{OffsetDateTime, ZoneOffset}

import org.mellowtech.zero.model.Timer
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * @author msvens
  * @since 01/10/16
  */

/*trait UserEntityTable {

  protected val databaseService: DatabaseService
  import databaseService.driver.api._

  class Users(tag: Tag) extends Table[UserEntity](tag, "users") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def password = column[String]("password")

    def * = (id, username, password) <> ((UserEntity.apply _).tupled, UserEntity.unapply)
  }

  protected val users = TableQuery[Users]

}*/

class TimerDAO(protected val dbService: DbService){


  import dbService.driver.api._

  class TimerTable(tag: Tag) extends Table[Timer](tag, "timer"){

    implicit val OffsetDateTimeToTimestamp = MappedColumnType.base[OffsetDateTime, Timestamp](
      l => Timestamp.from(l.toInstant),
      t => OffsetDateTime.ofInstant(t.toInstant, ZoneOffset.UTC)
    )

    def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def start = column[Option[OffsetDateTime]]("start")
    def stop = column[Option[OffsetDateTime]]("stop")
    def desc = column[Option[String]]("description")
    def * = (id,title,start,stop,desc) <> (Timer.tupled, Timer.unapply _)
  }

  private val timers = TableQuery[TimerTable]

  def insert(t: Timer): Future[Option[Int]] = dbService.db.run(timers returning timers.map(_.id) += t)

  def get(id: Int): Future[Option[Timer]] = {
    val q = timers.filter(_.id === id)
    dbService.db.run(q.result.headOption)
  }

  def get(t: String): Future[Option[Timer]] = {
    val q = timers.filter(_.title === t)
    dbService.db.run(q.result.headOption)
  }

  def list: Future[Seq[Timer]] = {
    val q = for(t <- timers) yield t
    dbService.db.run(q.result)
  }



}

