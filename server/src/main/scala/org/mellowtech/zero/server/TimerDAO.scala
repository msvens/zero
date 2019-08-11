package org.mellowtech.zero.server

import java.time.temporal.{ChronoUnit}
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId, ZoneOffset}
import java.util.UUID

import org.mellowtech.zero.model.Timer

import scala.concurrent.Future

/**
  * @author msvens
  * @since 01/10/16
  */

class TimerDAO(protected val dbService: DbService){


  import dbService.driver.api._

  class TimerTable(tag: Tag) extends Table[Timer](tag, "ztimer"){

    implicit val ZoneIdToString = MappedColumnType.base[ZoneOffset, String](
      zoneId => zoneId.getId(),
      zoneString => ZoneOffset.of(zoneString)
    )

    def id = column[UUID]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def start = column[OffsetDateTime]("start")
    def stop = column[OffsetDateTime]("stop")
    def description = column[Option[String]]("description")
    def * = (id.?, title, start, stop, description) <> (Timer.tupled, Timer.unapply)
  }

  private val timers = TableQuery[TimerTable]


  def insert(title: String, start: OffsetDateTime, stop: Either[OffsetDateTime, Long],
             description: Option[String] = None): Future[UUID] = {
    val end = stop match {
      case Left(x) => x
      case Right(x) => start.plus(x, ChronoUnit.MILLIS)
    }
    require(end.isAfter(start))
    require(end.getOffset == start.getOffset)
    insert(Timer(None, title, start, end, description))
  }

  def insert(t: Timer): Future[UUID] = dbService.db.run(timers returning timers.map(_.id) += t)

  def get(id: UUID): Future[Option[Timer]] = {
    val q = timers.filter(_.id === id)
    dbService.db.run(q.result.headOption)
  }

  def get(title: String): Future[Option[Timer]] = {
    val q = timers.filter(_.title === title)
    dbService.db.run(q.result.headOption)
  }

  def list: Future[Seq[Timer]] = {
    val q = for(t <- timers) yield t
    dbService.db.run(q.result)
  }



}

