package com.mturk.models.pgdb

import scala.slick.driver.PostgresDriver.simple._
import scala.util.Try

object Triad {

  case class Triad(id: Option[Int], verb: String, assignedCategory: String, recognized: String,
                      timeTakenToRecognize: String, verbFromTheSameSide: String, verFromTheOppositeSide: String)

  class TriadTable(tag: Tag) extends Table[Triad](tag, "Triad") {
    def id = column[Option[Int]]("TRIAD_ID", O.PrimaryKey, O.AutoInc)
    def verb = column[String]("TRIAD_VERB")
    def assignedCategory = column[String]("TRIAD_ASSIGNED_CATEGORY")
    def recognized = column[String]("TRIAD_RECOGNIZED") //this is stored in cookie
    def timeTakenToRecognize = column[String]("TRIAD_TIME_TO_RECOGNIZE")
    def verbFromTheSameSide = column[String]("TRIAD_VERB_FROM_SAME_CATE")
    def verFromTheOppositeSide = column[String]("TRIAD_VERB_FROM_OPPO_CATE")

    def * = (id, verb, assignedCategory, recognized,
      timeTakenToRecognize, verbFromTheSameSide, verFromTheOppositeSide) <> (Triad.tupled, Triad.unapply)
  }

  val triads = TableQuery[TriadTable]

  def insert(triad: Triad)(implicit s: Session): Try[Triad] = {
    Try {
      val triadId = triads returning triads.map(_.id) += triad
      triad.copy(id = triadId)
    }
  }

}