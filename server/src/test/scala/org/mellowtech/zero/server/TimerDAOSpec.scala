package org.mellowtech.zero.server

import java.time.{Instant, ZoneId}

import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.mellowtech.zero.model.{Split, Timer}
import org.scalatest.{Assertion, AsyncFlatSpec, BeforeAndAfter, BeforeAndAfterAll, Matchers}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


class TimerDAOSpec extends AsyncFlatSpec with Matchers with BeforeAndAfter{

  //import scala.concurrent.ExecutionContext.global
  var timerDAO: TimerDAO = _

  before {
    timerDAO = new TimerDAO(DatabaseConfig.forConfig[JdbcProfile]("h2mem_dc"))
  }

  after {
    timerDAO.db.close()
  }

  val title = "title"
  val desc = Some("description")
  val duration: Long = 3600*1000
  val zone = ZoneId.of("UTC")

  def createAndInsert(): Future[Timer] = {
    //val now = Instant.now()
    timerDAO.createTables() flatMap (_ => {
      insertTimer()
    })
  }

  def insertTimer(title: String = title, desc: Option[String] = desc): Future[Timer] = {
    timerDAO.addTimer(None, title, Instant.now(), Right(duration), zone, desc)
  }

  def insertSplit(timer: Long, description: Some[String] = desc): Future[Split] = {
    timerDAO.addSplit(timer, Instant.now(), description)
  }

  def assertDefault(t: Timer): Assertion = {
    assert(t.title == title)
    assert(t.start.plusMillis(duration) == t.stop)
    assert(t.description == desc)
  }


  behavior of "TimerDAO"

  it should "create the right tables" in {
    for {
      _ <- timerDAO.createTables()
      tnames <- timerDAO.tableNames()
    } yield {
      assert(tnames.size == 3)
      assert(tnames.count(_.equalsIgnoreCase("users")) == 1)
      assert(tnames.count(_.equalsIgnoreCase("timer")) == 1)
      assert(tnames.count(_.equalsIgnoreCase("split")) == 1)
    }
  }

  behavior of "Timer Functions"

  it should "insert a new timer" in {
    createAndInsert().map(t => assertDefault(t))
  }

  it should "retrive an existing timer using its id" in {
    for {
      t <- createAndInsert()
      t1 <- timerDAO.get(t.id)
    } yield {
      assert(t1.isDefined)
      assert(t1.get.id == t.id)
    }
  }

  it should "delete a timer using its id" in {
    for {
      t <- createAndInsert()
      _ <- timerDAO.deleteTimer(t.id)
      n <- timerDAO.timersSize()
    } yield {
      assert(n == 0)
    }
  }

  it should "retrive an existing timer using its title" in {
    for {
      t <- createAndInsert()
      t1 <- timerDAO.get(t.title)
    } yield {
      assert(t1.isDefined)
      assert(t1.get.id == t.id)
    }
  }

  it should "retrive a None for a non existent timer" in {
    for {
      _ <- timerDAO.createTables()
      t <- timerDAO.get(0)
    } yield {
      assert(t.isEmpty)
    }
  }

  it should "retrive all timers" in {
    for {
      t <- createAndInsert()
      t1 <- insertTimer("timer 2", Some("description 2"))
      timers <- timerDAO.list
    } yield {
      assert(timers.size == 2)
    }
  }

  it should "retrive zero timers if none is in the database" in {
    for {
      _ <- timerDAO.createTables()
      timers <- timerDAO.list
    } yield {
      assert(timers.size == 0)
    }
  }

  it should "delete any splits when deleting the timer" in {
    for {
      t <- createAndInsert()
      _ <- insertSplit(t.id)
      _ <- insertSplit(t.id)
      _ <- timerDAO.deleteTimer(t.id)
      n1 <- timerDAO.timersSize()
      n2 <- timerDAO.splitsSize()
    } yield {
      assert(n1 == 0)
      assert(n2 == 0)
    }
  }

  it should "fail if inserting timers with the same id" in {
    val inst = Instant.now
    val zone = ZoneId.of("UTC")
    val timer = Timer(1L, None, "new timer", inst, inst.plusSeconds(3600), zone, None)
    for {
      _ <- timerDAO.createTables()
      t <- timerDAO.addTimer(timer)
      e: Assertion <- recoverToSucceededIf[JdbcSQLIntegrityConstraintViolationException]{timerDAO.addTimer(timer)}
    } yield {
      e
    }
  }

  behavior of "Split Functions"

  it should "add a split to an existing timer" in {
    for {
      t <- createAndInsert()
      s <- insertSplit(t.id)
    } yield {
      assert(s.timer == t.id)
    }
  }

  it should "retrive the splits in a timer" in {
    for {
      t <- createAndInsert()
      s <- insertSplit(t.id)
      s1 <- insertSplit(t.id)
      splits <- timerDAO.getSplits(t.id)
    } yield {
      assert(splits.size == 2)
    }
  }

  it should "delete an existing split by id" in {
    for {
      t <- createAndInsert()
      s <- insertSplit(t.id)
      _ <- timerDAO.deleteSplit(s.id)
      n <- timerDAO.splitsSize()
    } yield {
      assert(n == 0)
    }
  }

  it should "retrive no splits for a timer with no splits" in {
    for {
      t <- createAndInsert()
      splits <- timerDAO.getSplits(t.id)
    } yield {
      assert(splits.size == 0)
    }
  }

  it should "retrive no splits for a non existent timer" in {
    for {
      t <- createAndInsert()
      splits <- timerDAO.getSplits(0)
    } yield {
      assert(splits.size == 0)
    }
  }

  it should "fail if inserting split to a non existent timer" in {
    for {
      t <- createAndInsert()
      a <- recoverToSucceededIf[JdbcSQLIntegrityConstraintViolationException](insertSplit(0))
    } yield {
      a
    }
  }



}
