package com.mturk.tasks.admin

import akka.actor.{ActorLogging, Actor}
import org.json4s.JsonAST.JObject

class AdminActor extends Actor with ActorLogging {
  import AdminProtocol._
  import com.mturk.tasks.Util._

  def receive = {
    case CreateAdmin(jObject) =>

  }
}

object AdminProtocol {
  case class CreateAdmin(jObject: JObject)
}