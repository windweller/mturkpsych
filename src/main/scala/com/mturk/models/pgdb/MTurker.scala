package com.mturk.models.pgdb

import java.sql.Timestamp
import java.util.UUID

import com.github.nscala_time.time.Imports._

import scala.slick.driver.PostgresDriver.simple._


object MTurker {
  case class MTurker(id: Option[Int], mturkId: Option[String], countTask: Int, commToken: String, timeStamp: Timestamp)

  class MTurkerTable(tag: Tag) extends Table[MTurker](tag, "MTurker") {
    def id = column[Option[Int]]("MTURKER_ID", O.PrimaryKey, O.AutoInc)
    def mturkId = column[Option[String]]("MTURKER_MTURK_ID")
    def countTask = column[Int]("MTURKER_COUNT_TASK")
    def commToken = column[String]("MTURKER_COMM_TOKEN") //this is stored in cookie
    def timeStamp = column[Timestamp]("MTURKER_TIME_STAMP")

    def * = (id, mturkId, countTask, commToken, timeStamp) <> (MTurker.tupled, MTurker.unapply)
  }

  val mTurkers = TableQuery[MTurkerTable]

  def insert()(implicit s: Session): (Option[MTurker], Boolean, Option[String]) = {
    val uuid = UUID.randomUUID().toString
    val time = new Timestamp(DateTime.now.getMillis)
    val mTurker = MTurker(None, None, 0, uuid, time)

    val mTurkerId = (mTurkers returning mTurkers.map(_.id)) += mTurker.copy(commToken = uuid)
    (Some(mTurker.copy(id=mTurkerId, commToken = uuid)), true, None)
  }

  /**
   * Throw excpetion
   * @param token
   * @param mTurkId
   * @param s
   * @return
   */
  def updateMTurkerId(token: String, mTurkId: String)(implicit s: Session): MTurker = {
    val q = for (m <- mTurkers if m.commToken === token) yield m
    q.list() match {
      case Nil => throw new Exception("id is not valid")
      case list =>
        q.update(list.head.copy(mturkId = Some(mTurkId)))
        list.head.copy(mturkId = Some(mTurkId))
    }
  }

  /**
   * This will raise exceptions instead of returning a tuple
   * @param mTurkId
   * @param s
   * @return
   */
  def getByMTurkerId(mTurkId: String)(implicit s: Session) = {
    val q = for (m <- mTurkers if m.mturkId === mTurkId) yield m
    q.list() match {
      case Nil => throw new Exception("There is no such mTurkId")
      case list => list.head
    }
  }

  def getByToken(token: String)(implicit s: Session): (Option[MTurker], Boolean, Option[String]) = {
    val q = for (m <- mTurkers if m.commToken === token) yield m
    q.list() match {
      case Nil => (None, false, Some("there is no such communication token, authorization failed"))
      case list => (Some(list.head),true, None)
    }
  }

  def completeOneTask(token: String)(implicit s: Session): (Option[MTurker], Boolean, Option[String]) = {
    val q = for (m <- mTurkers if m.commToken === token) yield m
    q.list() match {
      case Nil => (None, false, Some("there is no such mTurk Id, authorization failed"))
      case list =>
        val task = list.head.countTask
        q.update(list.head.copy(countTask = task + 1))

        (Some(list.head.copy(countTask = task + 1)), true, None)
    }
  }

}
