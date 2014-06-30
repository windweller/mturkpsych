package com.mturk.tasks.SECcompany

import com.mturk.Config
import akka.actor.{ActorLogging, Actor, ActorRef, ActorSystem}
import spray.http.StatusCodes
import spray.routing.Directives

//TODO: fix/complete this Article
class SECCompanyService(companyActor: ActorRef)(implicit system : ActorSystem) extends Directives {
  lazy val route =
    pathPrefix("article") {
      get {
        complete {
          //return article from DB
          "Article back"
        }
      } ~
      post {
        complete {
          //add article to DB
          ""
        }
      } ~
      put {
        complete {
          //change existing articles
          ""
        }
      }
    }
}

//This object stores message protocols
object SECCompanyProtocol {

}

class SECCompanyActor extends Actor with ActorLogging {
  def receive = {
    case _ => log.info("...")
  }
}