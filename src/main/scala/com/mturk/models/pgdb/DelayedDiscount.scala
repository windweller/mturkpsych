package com.mturk.models.pgdb

import scala.slick.driver.PostgresDriver.simple._
import scala.util.Try

object DelayedDiscount {

  //todayLeftOrRight is either left or right
  case class DelayedDiscount(id: Option[Int], commToken: String, version: String, phase: String, response: String, todayLeftOrRight: String,
                             timeIntervalForDelay: String, dollarToday: String, reactionTime: String)

  class DelayedTable(tag: Tag) extends Table[DelayedDiscount](tag, "DelayedDiscount") {
    def id = column[Option[Int]]("DELAYED_ID", O.PrimaryKey, O.AutoInc)
    def commToken = column[String]("DELAYED_COMM_TOKEN")
    def version = column[String]("DELAYED_VERSION")
    def phase = column[String]("DELAYED_PHASE")
    def response = column[String]("DELAYED_RESPONSE")
    def todayLeftOrRight = column[String]("DELAYED_TODAY_LEFT_OR_RIGHT")
    def timeIntervalForDelay = column[String]("DELAYED_TIME_INTERVAL_FOR_DELAY")
    def dollarToday = column[String]("DELAYED_DOLLAR_TODAY")
    def reactionTime = column[String]("DELAYED_REACTION_TIME")

    def * = (id, version, commToken, phase, response, todayLeftOrRight, timeIntervalForDelay, dollarToday,
       reactionTime) <> (DelayedDiscount.tupled, DelayedDiscount.unapply)
  }

  val delayDiscounts = TableQuery[DelayedTable]

  def insert(delayed: DelayedDiscount)(implicit s: Session): Try[DelayedDiscount] = {
    Try {
      val delayedId = delayDiscounts returning delayDiscounts.map(_.id) += delayed
      delayed.copy(id = delayedId)
    }
  }
}