package org.mellowtech.zero.db

import java.time.{Instant, ZoneId}

import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.mellowtech.zero.model.{Split, Timer, User}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, BeforeAndAfter}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.Future


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
  val name = "name"
  val email = "some@email.com"

  def createAndInsertTimer(): Future[Timer] = {
    //val now = Instant.now()
    timerDAO.createTables() flatMap (_ => {
      insertTimer()
    })
  }

  def createAndInsertUser(): Future[User] = {
    timerDAO.createTables() flatMap (_ => {insertUser()})
  }

  def insertUser(name: String = name, email: String = email, token: Option[String] = None): Future[User] = {
    timerDAO.addUser(name, email, token)
  }

  def insertTimer(title: String = title, desc: Option[String] = desc, userId: Option[Long] = None): Future[Timer] = {
    timerDAO.addTimer(userId, title, Instant.now(), Right(duration), zone, desc)
  }

  def insertSplit(timer: Long, description: Some[String] = desc): Future[Split] = {
    timerDAO.addSplit(timer, Instant.now(), description)
  }

  def assertDefaultTimer(t: Timer): Assertion = {
    assert(t.title == title)
    assert(t.start.plusMillis(duration) == t.stop)
    assert(t.description == desc)
  }

  def assertDefaultUser(u: User): Assertion = {
    assert(u.username == name)
    assert(u.email == email)
    assert(u.token == None)
  }

  def testGetUser(getU: User => Future[Option[User]]): Future[Assertion] = for {
    u <- createAndInsertUser()
    u2 <- getU(u)
  } yield {
    assert(u2.isDefined)
    assert(u2.get.id == u.id)
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

  behavior of "Users"

  it should "insert a new user" in {
    createAndInsertUser().map(assertDefaultUser)
  }

  it should "retrieve an existing user using its id" in {
    testGetUser(u => {timerDAO.getUser(u.id)})
  }

  it should "retrive an existing user by its name" in {
    testGetUser(u => {timerDAO getUserByName u.username})
  }

  it should "retrive an exisiting user ty its email" in {
    testGetUser(u => {timerDAO getUserByEmail u.email})
  }

  it should "retrive None when getting a nonexistent user" in {
    for {
      _ <- timerDAO.createTables()
      u <- timerDAO.getUser(0)
    } yield {
      assert(u == None)
    }
  }

  it should "retrive all users" in {
    for {
      _ <- createAndInsertUser()
      _ <- insertUser("name2", "some2@email.com")
      users <- timerDAO.listUsers()
    } yield {
      assert(users.size == 2)
    }
  }

  it should "retrive an empty seq when no users has been created" in {
    for {
      _ <- timerDAO.createTables()
      l <- timerDAO.listUsers()
    } yield {
      assert(l.isEmpty)
    }
  }

  it should "return a size of 1 with 1 user" in {
    for {
      _ <- createAndInsertUser()
      s <- timerDAO.usersSize()
    } yield {
      assert(s == 1)
    }
  }

  it should "delete a user using its id" in {
    for {
      u <- createAndInsertUser()
      c <- timerDAO.deleteUser(u.id)
    } yield {
      assert(c == 1)
    }
  }

  it should "cascade deletation of a user to asssociated timers and splits" in {
    for {
      u <- createAndInsertUser()
      t <- insertTimer("timer", Some("desc"), Some(u.id))
      s <- insertSplit(t.id, Some("split"))
      c <- timerDAO.deleteUser(u.id)
      s1 <- timerDAO.splitsSize()
      s2 <- timerDAO.timersSize()
    } yield {
      assert(c == 1)
      assert(s1 == 0)
      assert(s2 == 0)
    }
  }

  it should "fail when adding a user with the same name" in {
    for {
      u <- createAndInsertUser()
      e: Assertion <- recoverToSucceededIf[JdbcSQLIntegrityConstraintViolationException]{insertUser()}
    } yield {
      e
    }
  }

  it should "fail when adding a non-existent user to a new timer" in {
    timerDAO.createTables().flatMap(u => {
      recoverToSucceededIf[JdbcSQLIntegrityConstraintViolationException]{insertTimer("newtimer", Some("desc"), Some(0))}
    })
  }



  behavior of "Timer Functions"

  it should "insert a new timer" in {
    createAndInsertTimer().map(assertDefaultTimer)
  }

  it should "retrive an existing timer using its id" in {
    for {
      t <- createAndInsertTimer()
      t1 <- timerDAO.getTimer(t.id)
    } yield {
      assert(t1.isDefined)
      assert(t1.get.id == t.id)
    }
  }

  it should "delete a timer using its id" in {
    for {
      t <- createAndInsertTimer()
      c <- timerDAO.deleteTimer(t.id)
    } yield {
      assert(c == 1)
    }
  }

  it should "retrive an existing timer using its title" in {
    for {
      t <- createAndInsertTimer()
      t1 <- timerDAO.getTimerByTitle(t.title)
    } yield {
      assert(t1.isDefined)
      assert(t1.get.id == t.id)
    }
  }

  it should "retrive a None for a non existent timer" in {
    for {
      _ <- timerDAO.createTables()
      t <- timerDAO.getTimer(0)
    } yield {
      assert(t.isEmpty)
    }
  }

  it should "retrive all timers" in {
    for {
      _ <- createAndInsertTimer()
      _ <- insertTimer("timer 2", Some("description 2"))
      timers <- timerDAO.listTimers()
    } yield {
      assert(timers.size == 2)
    }
  }

  it should "retive all timers belongining to a user" in {
    for {
      u <- createAndInsertUser()
      _ <- insertTimer(userId = Some(u.id))
      _ <- insertTimer(userId = Some(u.id))
      t <- timerDAO.getTimersByUser(u.id)
    } yield {
      assert(t.size == 2)
    }
  }

  it should "retrive zero timers if none is in the database" in {
    for {
      _ <- timerDAO.createTables()
      timers <- timerDAO.listTimers()
    } yield {
      assert(timers.size === 0)
    }
  }

  it should "delete any splits when deleting the timer" in {
    for {
      t <- createAndInsertTimer()
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
      t <- createAndInsertTimer()
      s <- insertSplit(t.id)
    } yield {
      assert(s.timer == t.id)
    }
  }

  it should "retrive the splits in a timer" in {
    for {
      t <- createAndInsertTimer()
      s <- insertSplit(t.id)
      s1 <- insertSplit(t.id)
      splits <- timerDAO.getSplits(t.id)
    } yield {
      assert(splits.size == 2)
    }
  }

  it should "delete an existing split by id" in {
    for {
      t <- createAndInsertTimer()
      s <- insertSplit(t.id)
      _ <- timerDAO.deleteSplit(s.id)
      n <- timerDAO.splitsSize()
    } yield {
      assert(n == 0)
    }
  }

  it should "retrive no splits for a timer with no splits" in {
    for {
      t <- createAndInsertTimer()
      splits <- timerDAO.getSplits(t.id)
    } yield {
      assert(splits.isEmpty)
    }
  }

  it should "retrive no splits for a non existent timer" in {
    for {
      t <- createAndInsertTimer()
      splits <- timerDAO.getSplits(0)
    } yield {
      assert(splits.isEmpty)
    }
  }

  it should "fail if inserting split to a non existent timer" in {
    for {
      t <- createAndInsertTimer()
      a <- recoverToSucceededIf[JdbcSQLIntegrityConstraintViolationException](insertSplit(-1))
    } yield {
      a
    }
  }



}
