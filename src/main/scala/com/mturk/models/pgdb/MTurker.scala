package com.mturk.models.pgdb

import java.util.UUID

import scala.slick.driver.PostgresDriver.simple._


object MTurker {
  case class MTurker(id: Option[Int], countTask: Int, commToken: String)

  class MTurkerTable(tag: Tag) extends Table[MTurker](tag, "MTurker") {
    def id = column[Option[Int]]("MTURKER_ID", O.PrimaryKey, O.AutoInc)
    def countTask = column[Int]("MTURKER_COUNT_TASK")
    def commToken = column[String]("MTURKER_COMM_TOKEN") //this is stored in cookie

    def * = (id, countTask, commToken) <> (MTurker.tupled, MTurker.unapply)
  }

  val mTurkers = TableQuery[MTurkerTable]

  def insert()(implicit s: Session): (Option[MTurker], Boolean, Option[String]) = {
    val uuid = UUID.randomUUID().toString
    val mTurker = MTurker(None, 0, uuid)

    val mTurkerId = (mTurkers returning mTurkers.map(_.id)) += mTurker.copy(commToken = uuid)
    (Some(mTurker.copy(id=mTurkerId, commToken = uuid)), true, None)
  }

  def getByToken(token: String)(implicit s: Session): (Option[MTurker], Boolean, Option[String]) = {
    val q = for (m <- mTurkers if m.commToken === token) yield m
    q.list() match {
      case Nil => (None, false, Some("there is no such communication token, authorization failed"))
      case list => (Some(list.head),true, None)
    }
  }

  def completeOneTask(token: String): (Option[MTurker], Boolean, Option[String]) = {
    val q = for (m <- mTurkers if m.commToken === token) yield m
    q.list() match {
      case Nil => (None, false, Some("there is no such communication token, authorization failed"))
      case list =>
        val task = list.head.countTask
        q.update(list.head.copy(countTask = task + 1))

        (Some(list.head.copy(countTask = task + 1)), true, None)
    }
  }

}
