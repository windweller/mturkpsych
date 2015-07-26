package com.mturk.tasks.futureDemo

import java.io.File

import akka.actor._
import akka.util.Timeout
import com.mturk.tasks.Util.JsonImplicits
import com.mturk.tasks.futureDemo.FutureDemoProtocol.{TransOk, JObjectSentences}
import org.json4s.JsonAST.JObject
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.routing.Directives
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

/**
 * Created by Aimingnie on 7/20/15
 */
class FutureDemoService(futureDemoActor: ActorRef)(implicit system: ActorSystem)
  extends Directives {

  import scala.concurrent.duration._
  import akka.pattern.ask
  import JsonImplicits._

  implicit val timeout = Timeout(45 seconds)

  lazy val route = pathPrefix("future") {
    path("data") {
      get {
        getFromFile(new File("views/js/data1.json"), `application/json`)
      }
    } ~
    path("sentence") {
      post {
        entity(as[JObject]) { jObject =>
          val response = (futureDemoActor ? JObjectSentences(jObject))
                        .mapTo[TransOk]
                        .map(result => result.succeedOrNot match {
                          case true => (OK, result.sentencesResult.get)
                          case false => (Unauthorized, result.errorMessage)
                        })
          complete(response)
        }
      }
    }
  }
}
