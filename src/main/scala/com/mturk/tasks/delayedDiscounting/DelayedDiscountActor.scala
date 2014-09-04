package com.mturk.tasks.delayedDiscounting

import akka.actor.{ActorLogging, Actor}
import org.json4s.JsonAST.JObject
import com.mturk.models.pgdb._
import com.mturk.tasks.Util._
import scala.util.{Success,Failure}

class DelayedDiscountActor extends Actor with ActorLogging {
  import com.mturk.tasks.delayedDiscounting.DelayedDiscountActorProtocol._


//  case class DelayedDiscount(id: Option[Int], commToken: String, version: String response: String, todayLeftOrRight: String,
//                             timeIntervalForDelay: String, dollarToday: String, dollarForDelay: String, reactionTime: String)
  def receive = {
    case JObjectTriadResult(jObject) =>
      val fields = List("commToken","version", "chosenTodayOrNot", "phase", "response", "todayLeftOrRight", "timeIntervalForDelay",
        "dollarToday", "reactionTime")

      extractJObjectEntity(fields, jObject) match {
        case Right(fieldsValue) =>
          DAL.db.withSession { implicit session =>
            val result = DelayedDiscount.insert(DelayedDiscount.DelayedDiscount(None, fieldsValue(0), fieldsValue(1),
              fieldsValue(2), fieldsValue(3), fieldsValue(4), fieldsValue(5), fieldsValue(6), fieldsValue(7), fieldsValue(8)))
            result match {
              case Success(t) => sender ! TransOk(Some(t), None)
              case Failure(ex) => sender ! TransOk(None, Some(ex.getMessage))
            }
          }
        case Left(errorString) =>
          sender ! TransOk(None, Some(errorString))
      }
  }
}

object DelayedDiscountActorProtocol {
  case class JObjectTriadResult(jObject: JObject)
  case class TransOk(triad: Option[DelayedDiscount.DelayedDiscount], errorMessage: Option[String])
}