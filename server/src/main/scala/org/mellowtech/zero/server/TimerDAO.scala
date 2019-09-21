package org.mellowtech.zero.server

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}

import org.mellowtech.zero.model.{Split, Timer, User}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * @author msvens
  * @since 01/10/16
  */


class TimerDAO(protected val config: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext){


  import config.profile.api._

  val db = config.db

  val splitIdGenerator = new DefaultIdGenerator(1, 1)
  val timerIdGenerator = new DefaultIdGenerator(2, 1)
  val userIdGenerator = new DefaultIdGenerator(3, 1)

  class UserTable(tag: Tag) extends Table[User](tag, "users"){
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey)
    def username: Rep[String] = column[String]("username", O.Unique)
    def email: Rep[String] = column[String]("email", O.SqlType("VARCHAR(256)"))
    def * = (id, username, email) <>(User.tupled, User.unapply)
  }

  class TimerTable(tag: Tag) extends Table[Timer](tag, "timer"){

    implicit val ZoneIdToString = MappedColumnType.base[ZoneId, String](
      zoneId => zoneId.getId,
      zoneString => ZoneId.of(zoneString)
    )

    def id = column[Long]("id", O.PrimaryKey)
    def user = column[Option[Long]]("user")
    def title = column[String]("title")
    def start = column[Instant]("start")
    def stop = column[Instant]("stop")
    def zone = column[ZoneId]("timezone")
    def description = column[Option[String]]("description")

    def titleIdx = index("idx_title", (title), unique = false)

    def * = (id, user, title, start, stop, zone, description) <> (Timer.tupled, Timer.unapply)

  }



  class SplitTable(tag: Tag) extends Table[Split](tag, "split"){

    def id = column[Long]("id", O.PrimaryKey)
    def timer = column[Long]("timer")
    def stamp = column[Instant]("stamp")
    def description = column[Option[String]]("description")
    def timerfk = foreignKey("fk_timer", timer, timers)(_.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)

    def * = (id, timer, stamp, description) <> (Split.tupled, Split.unapply)

  }

  private val users = TableQuery[UserTable]
  private val timers = TableQuery[TimerTable]
  private val splits = TableQuery[SplitTable]

  //Precompiled Queries:
  val timerByIdCompiled = Compiled((id: Rep[Long]) => timers.filter(_.id === id))

  //Schema functions
  val createDbTables: DBIOAction[Unit, NoStream, Effect.Schema] = DBIO.seq(
    (users.schema ++ timers.schema ++ splits.schema).create
  )

  val dropDbTables = DBIO.seq(
    (users.schema ++ timers.schema ++ splits.schema).dropIfExists
  )

  def printCreateStatements(): Unit = {
    println((users.schema ++ timers.schema ++ splits.schema).createStatements.mkString("\n"))
  }

  def createTables(): Future[Unit] = {
    db.run(createDbTables)
  }

  def tableNames(): Future[Seq[String]] = for {
    tables <- db.run(MTable.getTables)
  } yield tables.map(t => t.name.name)

  def createTablesSynced(): Unit = {
    Await.result(createTables(), 1.second)
  }

  def dropTables(): Future[Unit] = {
    db.run(dropDbTables)
  }

  def timersSize(): Future[Int] = {
    db.run(timers.size.result)
  }

  def splitsSize(): Future[Int] = {
    db.run(splits.size.result)
  }


  def addSplit(timer: Long, time: Instant, description: Option[String] = None): Future[Split] = {
    addSplit(Split(splitIdGenerator.nextId(), timer, time, description))
  }

  private def addSplit(s: Split): Future[Split] = {
    config.db.run(splits += s).map(_ => s)
  }

  def deleteSplit(id: Long): Future[Int] = {
    val q = splits.filter(_.id === id)
    db.run(q.delete)
  }

  def getSplits(timerId: Long): Future[Seq[Split]] = {
    val q = splits.filter(_.timer === timerId)
    config.db.run(q.result)
  }

  def addTimer(user: Option[Long], title: String, start: Instant, duration: Either[Instant, Long],
             zone: ZoneId, description: Option[String] = None): Future[Timer] = {
    val stop = duration match {
      case Left(x) => x
      case Right(x) => start.plus(x, ChronoUnit.MILLIS)
    }
    addTimer(Timer(timerIdGenerator.nextId(), user, title, start, stop, zone, description))
  }

  def deleteTimer(id: Long): Future[Int] = {
    //db.run(timers.size)
    val q = timers.filter(_.id === id)
    db.run(q.delete)
  }

  private[server] def addTimer(t: Timer): Future[Timer] = for {
      _ <- Future(require(t.stop.isAfter(t.start)), "Stop is not after start")
      cols: Int <- db.run(timers += t)
    } yield t

  def get(id: Long): Future[Option[Timer]] = {
    db.run(timerByIdCompiled(id).result.headOption)
  }

  def get(title: String): Future[Option[Timer]] = {
    val q = timers.filter(_.title === title)
    db.run(q.result.headOption)
  }

  def list(): Future[Seq[Timer]] = {
    val q = for(t <- timers) yield t
    db.run(q.result)
  }




}

