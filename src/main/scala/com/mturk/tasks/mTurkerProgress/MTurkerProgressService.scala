package com.mturk.tasks.mTurkerProgress

import akka.actor.{ActorSystem, ActorRef}
import com.mturk.tasks.Util.Authenticator
import spray.routing.Directives


class MTurkerProgressService (MTurkerProgressActor: ActorRef)(implicit system: ActorSystem)
  extends Directives with Authenticator {

  import akka.pattern.ask
  import com.mturk.tasks.mTurkerProgress.MTurkerProgressProtocol._
  import spray.http.StatusCodes._
  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeout = Timeout(5 seconds)

  lazy val route =
    path("mturker") {
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
              case true => (OK, result.mturker.get)
              case false => (BadRequest, result.errorMessage)
            })
            complete(response)
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
      }
    }
}