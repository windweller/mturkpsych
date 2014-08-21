package com.mturk.tasks.triadTest

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import org.json4s.JsonAST.JObject
import spray.http.StatusCodes._
import spray.routing.Directives
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TriadTestService(triadTestActor: ActorRef)(implicit system: ActorSystem)
  extends Directives {

  import com.mturk.tasks.triadTest.TriadTestActorProtocol._
  import akka.pattern.ask
  import com.mturk.tasks.Util._
  import JsonImplicits._

  implicit val timeout = Timeout(5 seconds)

  lazy val route =
    pathPrefix("triad") {
      path("help") {
        hostName { hn =>
          val localhostPortConfig = if (hn == "127.0.0.1") ":8080" else ""
          val httpPrefix = if (hn == "127.0.0.1") "http://" else "https://"

          val routeApis = Map[String, (String, String)](
            "triadResultUpload" -> ("POST", httpPrefix +  hn + localhostPortConfig  + "/triad/result/")
          )
          complete(routeApis)
        }
      } ~
      pathPrefix("result") {
        post {
          entity(as[JObject]) { jObject =>
            optionalCookie("commToken") {
              case None =>
                complete(Unauthorized, "Please retrieve and add your communication token as cookie.")
              case Some(commToken) =>
                val authInfo = getUser(commToken.content)
                if (!authInfo.accepted) complete((Unauthorized, "Communication Token authentication not passed."))

                val response = (triadTestActor ? JObjectTriadResult(jObject))
                  .mapTo[TransOk]
                  .map(result => result.errorMessage match {
                  case None => (OK, result.triad.get)
                  case Some(ex) => (BadRequest, ex)
                })
                complete(response)
            }
          }
        }
      }


    }

}
