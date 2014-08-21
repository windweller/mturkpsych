package com.mturk.models.pgdb

import scala.slick.driver.PostgresDriver.simple._
import scala.util.Try

object Triad {

//  val fields = List("verbTop", "verbTopCate", "verbLeft", "verbLeftCate",
//    "verbRight", "verbRightCate", "predict", "reactionTime", "response")

  case class Triad(id: Option[Int], commToken: String, phase: String, verbTop: String, verbTopCate: String, verbLeft: String,
                      verbLeftCate: String, verbRight: String, verbRightCate: String,
                      predict: String, reactionTime: String, response: String)

  class TriadTable(tag: Tag) extends Table[Triad](tag, "Triad") {
    def id = column[Option[Int]]("TRIAD_ID", O.PrimaryKey, O.AutoInc)
    def commToken = column[String]("TRIAD_COMM_TOKEN")
    def phase = column[String]("TRIAD_PHASE")
    def verbTop = column[String]("TRIAD_VERB_TOP")
    def verbTopCate = column[String]("TRIAD_VERB_TOP_CATE")
    def verbLeft = column[String]("TRIAD_VERB_LEFT") //this is stored in cookie
    def verbLeftCate = column[String]("TRIAD_VERB_LEFT_CATE")
    def verbRight = column[String]("TRIAD_VERB_RIGHT")
    def verbRightCate = column[String]("TRIAD_VERB_RIGHT_CATE")
    def predict = column[String]("TRIAD_PREDICT")
    def reactionTime = column[String]("TRIAD_REACTION_TIME")
    def response = column[String]("TRIAD_RESPONSE")

    def * = (id, commToken, phase, verbTop, verbTopCate, verbLeft,
      verbLeftCate, verbRight, verbRightCate, predict,
      reactionTime, response) <> (Triad.tupled, Triad.unapply)
  }

  val triads = TableQuery[TriadTable]

  def insert(triad: Triad)(implicit s: Session): Try[Triad] = {
    Try {
      val triadId = triads returning triads.map(_.id) += triad
      triad.copy(id = triadId)
    }
  }

}