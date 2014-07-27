package com.mturk.tasks.mTurkerProgress

import akka.actor.{ActorSystem, ActorRef}
import com.mturk.tasks.Util._
import spray.http.HttpCookie
import spray.routing.Directives
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import scala.concurrent.duration._


class MTurkerProgressService (MTurkerProgressActor: ActorRef)(implicit system: ActorSystem)
  extends Directives with Authenticator {

  import akka.pattern.ask
  import com.mturk.tasks.mTurkerProgress.MTurkerProgressProtocol._
  import spray.http.StatusCodes._
  import JsonImplicits._
  import org.json4s.JsonAST.JObject

  implicit val timeout = Timeout(5 seconds)

  lazy val route =
    pathPrefix("mturker") {
      pathEndOrSingleSlash {
        get {
          optionalCookie("commToken") {
            case Some(commToken) =>
              val response = (MTurkerProgressActor ? CommToken(commToken.content))
                .mapTo[TransOk]
                .map(result => result.succeedOrNot match {
                case true => (OK, result.mturker.get)
                case false => (Unauthorized, result.errorMessage)
              })
              complete(response)
            case None =>

              val response = (MTurkerProgressActor ? Register)
                .mapTo[TransOk]
                .map(result => result.succeedOrNot match {
                case true => (OK, result)
                case false => (BadRequest, result)
              })

              //TODO: Change this ugly blocking code in the future
              val result = Await.result(response, 5 seconds)

              setCookie(HttpCookie("commToken", content = result._2.mturker.get.commToken),
                HttpCookie("countTask", content= result._2.mturker.get.countTask.toString)) {
                complete(result._1, result._2.mturker.get)
              }

          }
        } ~
          post {
            optionalCookie("commToken") {
              case None =>
                complete(Unauthorized, "Please retrieve and add your communication token as cookie.")
              case Some(commToken) =>
                val response = (MTurkerProgressActor ? CompleteOneTask(commToken.content))
                  .mapTo[TransOk]
                  .map(result => result.succeedOrNot match {
                  case true => (OK, result.mturker.get)
                  case false => (Unauthorized, result.errorMessage)
                })
                complete(response)
            }
          } ~
          //use PUT to update mTurker's ID
          put {
            entity(as[JObject]) { jObject =>
              optionalCookie("commToken") {
                case None =>
                  complete(Unauthorized, "Please retrieve and add your communication token as 'commToken' cookie.")
                case Some(commToken) =>
                  val response = (MTurkerProgressActor ? UpdateMturkID(commToken.content, jObject))
                    .mapTo[TransOk]
                    .map(message => (OK, message.mturker.get))
                    .recover{
                    case e => (BadRequest, e.getMessage)
                  }
                  complete(response)
              }
            }
          }
      } ~
        pathPrefix("help") {
          pathEnd {
            hostName { hn =>
              val localhostPortConfig = if (hn == "127.0.0.1") ":8080" else ""
              val routeApis = Map[String, (String, String)](
                "mTurkerGet" -> ("GET", "http://" + hn + localhostPortConfig + "/mturker"),
                "mTurkerRegister" -> ("POST", "http://" + hn + localhostPortConfig + "/mturker")
              )
              complete(routeApis)
            }
          }
        }
    }
}