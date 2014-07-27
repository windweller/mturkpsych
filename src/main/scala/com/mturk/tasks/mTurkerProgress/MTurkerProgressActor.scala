package com.mturk.tasks.mTurkerProgress

import akka.actor.{ActorLogging, Actor}
import org.json4s.JsonAST.JObject
import com.mturk.models.pgdb._

import scala.util.Try

class MTurkerProgressActor extends Actor with ActorLogging {

  import com.mturk.tasks.Util._
  import com.mturk.tasks.mTurkerProgress.MTurkerProgressProtocol._
  import JsonImplicits._
  import scala.util.{Success,Failure}


  def receive = {
    case Register =>
      val result = DAL.db.withSession{ implicit session =>
        MTurker.insert()
      }
      sender ! TransOk(result._1, result._2, result._3)

    case CommToken(token) =>
      val result = DAL.db.withSession { implicit session =>
        MTurker.getByToken(token)
      }
      sender ! TransOk(result._1, result._2, result._3)

    case CompleteOneTask(token) =>
      val result = DAL.db.withSession {implicit session =>
        MTurker.completeOneTask(token)
      }
      sender ! TransOk(result._1, result._2, result._3)

    case UpdateMturkID(id, jObject) =>
      val mTurkid = if (jObject.isDefined("mturkid")) throw new Exception("mturkid field in PUT request is missing")
        else jObject.getValue("mturkid")
      val result: Try[MTurker.MTurker] = DAL.db.withSession { implicit session =>
        Try(MTurker.updateMTurkerId(id, mTurkid))
      }
      result match {
        case Success(content) => sender ! TransOk(Some(content), succeedOrNot = true, None)
        case Failure(ex) => throw ex //let it escalate
      }
  }
}

object MTurkerProgressProtocol {
  case class CommToken(commToken: String)
  case class TransOk(mturker: Option[MTurker.MTurker], succeedOrNot: Boolean, errorMessage: Option[String])
  case class CompleteOneTask(commToken: String)
  case class UpdateMturkID(commToken: String, jObject: JObject)

  case object Register
}