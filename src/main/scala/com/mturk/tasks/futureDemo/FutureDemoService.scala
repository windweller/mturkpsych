package com.mturk.tasks.futureDemo

import java.io.File

import akka.actor.{ActorSystem, ActorRef}
import spray.http.MediaTypes._
import spray.routing.Directives

/**
 * Created by Aimingnie on 7/20/15.
 */
class FutureDemoService(futureDemoActor: ActorRef)(implicit system: ActorSystem)
  extends Directives  {

  lazy val route = pathPrefix("future") {
    path("data") {
      get {
        getFromFile(new File("views/js/data1.json"), `application/json`)
      }
    }
  }
}
