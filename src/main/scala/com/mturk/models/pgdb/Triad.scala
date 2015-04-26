package com.mturk.models.pgdb

import scala.slick.driver.PostgresDriver.simple._
import scala.util.Try

object Triad {

//  val fields = List("verbTop", "verbTopCate", "verbLeft", "verbLeftCate",
//    "verbRight", "verbRightCate", "predict", "reactionTime", "response", "randomId")

  case class Triad(id: Option[Int], commToken: String, phase: String, verbTop: String, verbLeft: String,
                       verbRight: String, reactionTime: String, response: String, randomId: String)

  class TriadTable(tag: Tag) extends Table[Triad](tag, "Triad4") {
    def id = column[Option[Int]]("TRIAD_ID", O.PrimaryKey, O.AutoInc)
    def commToken = column[String]("TRIAD_COMM_TOKEN")
    def phase = column[String]("TRIAD_PHASE")
    def verbTop = column[String]("TRIAD_VERB_TOP")
    def verbLeft = column[String]("TRIAD_VERB_LEFT") //this is stored in cookie
    def verbRight = column[String]("TRIAD_VERB_RIGHT")
    def reactionTime = column[String]("TRIAD_REACTION_TIME")
    def response = column[String]("TRIAD_RESPONSE")
    def randomId = column[String]("TRIAD_EXPID")

    def * = (id, commToken, phase, verbTop, verbLeft,
       verbRight, reactionTime, response, randomId) <> (Triad.tupled, Triad.unapply)
  }

  val triads = TableQuery[TriadTable]

  def insert(triad: Triad)(implicit s: Session): Try[Triad] = {
    Try {
      val triadId = triads returning triads.map(_.id) += triad
      triad.copy(id = triadId)
    }
  }

}