package com.mturk.tasks.oAuthRelay

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import com.mturk.tasks.Util._
import JsonImplicits._
import org.json4s.JsonAST.JObject
import spray.routing.Directives
import SpotifyTokenSwapActorMsg._
import akka.pattern.ask
import spray.http.StatusCodes._
import scala.concurrent.duration._
import com.mturk.tasks.Util._
import scala.concurrent.ExecutionContext.Implicits.global


class SpotifyTokenSwapService (SpotifyTokenSwapActor: ActorRef)(implicit system: ActorSystem)
  extends Directives {

  implicit val timeout = Timeout(5 seconds)

  lazy val route =
    pathPrefix("oauthrelay") {
      pathPrefix("spotify") {
        post {
          entity(as[JObject]) { jObject =>

            if (jObject.isDefined("code")) {
              val response = (SpotifyTokenSwapActor ? Authenticate(jObject.getValue("code")))
                .mapTo[TransOk]
                .map(result => (OK, result.statusCode + ": " + result.text))
              complete(response)
            }else{
              val response = (SpotifyTokenSwapActor ? Authenticate(""))
                .mapTo[TransOk]
                .map(result => (OK, result.statusCode + ": " + result.text))
              complete(response)
            }

          }
        }
      }
    }

}
