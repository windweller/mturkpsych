package com.mturk.tasks.oAuthRelay

import akka.actor.{ActorLogging, Actor}
import spray.http.StatusCode
import scalaj.http.Http
import spray.http.StatusCodes._


class SpotifyTokenSwapActor extends Actor with ActorLogging {

  val k_client_id = "5d7764c105384573a6196774d4d4275f"
  val k_client_secret = "6afefd6a9bf24668a01ec0c329a01063"
  val k_client_callback_url = "play-with-coke-login://callback"

  import SpotifyTokenSwapActorMsg._

  def receive = {
    case Authenticate(code) =>
      val parameters = Map(
        "grant_type"-> "authorization_code",
        "client_id"-> k_client_id,
        "client_secret"-> k_client_secret,
        "redirect_uri"-> k_client_callback_url,
        "code" -> code
      )

      val content = "grant_type=authorization_code&client_id="+
                    k_client_id+"&client_secret="+k_client_secret+"&redirect_uri="+k_client_callback_url+
                    "&code=" + code
      val request: Http.Request = Http.postData("https://ws.spotify.com/oauth/token", content).header("content-type", "text/plain")
      val result = request.responseCode
      val text = request.asString

      sender ! TransOk(result, text)

  }
}

object SpotifyTokenSwapActorMsg {
  case class Authenticate(code: String)
  case class TransOk(statusCode: Int, text: String)
}                               f