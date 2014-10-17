package com.mturk.tasks.oAuthRelay

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import spray.routing.Directives
import SpotifyTokenSwapActorMsg._
import akka.pattern.ask
import spray.http.StatusCodes._
import scala.concurrent.duration._


class SpotifyTokenSwapService (SpotifyTokenSwapActor: ActorRef)(implicit system: ActorSystem)
  extends Directives {

  implicit val timeout = Timeout(5 seconds)

  lazy val route =
    pathPrefix("oauthrelay") {
      pathPrefix("spotify") {
        get {
          val response = (SpotifyTokenSwapActor ? Authenticate)
                      .mapTo[TransOk]
                      .map(result => (OK, result.statusCode + ": " + result.text))
          complete(response)
        }
      }
    }

}
