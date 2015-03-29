package com.mturk.tasks.triadTest

import akka.actor.{ActorLogging, Actor}
import org.json4s.JsonAST.JObject
import com.mturk.models.pgdb._
import com.mturk.tasks.Util._
import scala.util.{Success,Failure}


class TriadTestActor extends Actor with ActorLogging {

  import com.mturk.tasks.triadTest.TriadTestActorProtocol._

  def receive = {
    case JObjectTriadResult(jObject) =>
      val fields = List("commToken", "phase", "verbTop", "verbLeft",
        "verbRight", "reactionTime", "response", "sequence")

      extractJObjectEntity(fields, jObject) match {
        case Right(fieldsValue) =>
          DAL.db.withSession { implicit session =>
            val result = Triad.insert(Triad.Triad(None, fieldsValue.head, fieldsValue(1),
              fieldsValue(2), fieldsValue(3), fieldsValue(4), fieldsValue(5), fieldsValue(6), fieldsValue(7)))
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

object TriadTestActorProtocol {
  case class JObjectTriadResult(jObject: JObject)
  case class TransOk(triad: Option[Triad.Triad], errorMessage: Option[String])
}