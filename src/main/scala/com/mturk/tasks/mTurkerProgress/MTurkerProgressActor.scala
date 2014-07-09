package com.mturk.tasks.mTurkerProgress

import akka.actor.{ActorLogging, Actor}
import org.json4s.JsonAST.JObject
import com.mturk.models.pgdb._

class MTurkerProgressActor extends Actor with ActorLogging {

  import com.mturk.tasks.Util._
  import com.mturk.tasks.mTurkerProgress.MTurkerProgressProtocol._
  import JsonImplicits._

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
  }
}

object MTurkerProgressProtocol {
  case class CommToken(commToken: String)
  case class TransOk(mturker: Option[MTurker.MTurker], succeedOrNot: Boolean, errorMessage: Option[String])
  case class CompleteOneTask(commToken: String)

  case object Register
}