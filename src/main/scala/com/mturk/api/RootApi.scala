package com.mturk.api

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.event.Logging.InfoLevel
import com.mturk.tasks.SECcompany._
import com.mturk.tasks.delayedDiscounting.DelayedDiscountService
import com.mturk.tasks.mTurkerProgress.MTurkerProgressService
import com.mturk.tasks.triadTest.TriadTestService
import com.mturk.tasks.futureDemo.FutureDemoService
import spray.http.HttpRequest
import spray.http.MediaTypes._
import spray.http.StatusCodes.NotFound
import spray.httpx.encoding.Gzip
import spray.routing.directives.LogEntry
import spray.routing.{Directives, RouteConcatenation}

trait AbstractSystem {
  implicit def system: ActorSystem
}

//the ReactiveApi trait, which defines the REST endpoints. In keeping with the structure exposed above,
//each endpoint has been kept in its own class. The ReactiveApi trait constructs the classes for these
// endpoints and then concatenates their respective routes.
trait RootApi extends RouteConcatenation with StaticRoute with AbstractSystem {
  this: MainActors =>

  val rootService = system.actorOf(Props(new RootService(routes)))

  lazy val routes = logRequest(showReq _) {
//    new SECCompanyService(companyActor).route ~
//    new MTurkerProgressService(mTurkerActor).route ~
//    new TriadTestService(triadActor).route ~
//    new DelayedDiscountService(delayedDiscountActor).route ~
    new FutureDemoService(futureDemoActor).route ~
    staticRoute
  }

  private def showReq(req : HttpRequest) = LogEntry(req.uri, InfoLevel)
}

trait StaticRoute extends Directives {
  this: AbstractSystem =>

  lazy val staticRoute =
    pathEndOrSingleSlash {
//        getFromFile(new File("views/home.html"), `text/html`)
      getFromFile(new File("views/future.html"), `text/html`)
    } ~
//    path("sec") {
//      getFromFile(new File("views/sec.html"), `text/html`)
//    } ~
//    path("secturk") {
//      getFromFile(new File("views/secturk.html"), `text/html`)
//    } ~
//    path("triad") {
//      getFromFile(new File("views/triad.html"), `text/html`)
//    } ~
//    path("triadturk") {
//      getFromFile(new File("views/triadturk.html"), `text/html`)
//    } ~
    path("future") {
      getFromFile(new File("views/future.html"), `text/html`)
    } ~
    path("wordiverse5") {
      getFromFile(new File("views/wordiverse5.html"), `text/html`)
    } ~
    path("3dwordiverse") {
      getFromFile(new File("views/3dwordiverse.html"), `text/html`)
    } ~
    path("menuTemplate") {
      getFromFile(new File("views/menuTemplate.html"), `text/html`)
    } ~
    path("futureAnalysis") {
      getFromFile(new File("views/futureAnalysis.html"), `text/html`)
    } ~
    path("realTimeGraph") {
      getFromFile(new File("views/realTimeGraph.html"), `text/html`)
    } ~
    path("trainingCorpus") {
      getFromFile(new File("views/trainingCorpus.html"), `text/html`)
    } ~
    path("sourceCode") {
      getFromFile(new File("views/sourceCode.html"), `text/html`)
    } ~
//    path("delayeddiscountv1") {
//      getFromFile(new File("views/delayedDiscounting/delayedDiscountv1.html"), `text/html`)
//    } ~
//    path("delayeddiscountturkv1") {
//      getFromFile(new File("views/delayedDiscounting/delayedDiscountTurkv1.html"), `text/html`)
//    } ~
//    path("delayeddiscountv2") {
//      getFromFile(new File("views/delayedDiscounting/delayedDiscountv2.html"), `text/html`)
//    } ~
//    path("delayeddiscountturkv2") {
//      getFromFile(new File("views/delayedDiscounting/delayedDiscountTurkv2.html"), `text/html`)
//    } ~
//    path("delayeddiscountv3") {
//      getFromFile(new File("views/delayedDiscounting/delayedDiscountv3.html"), `text/html`)
//    } ~
//    path("delayeddiscountturkv3") {
//      getFromFile(new File("views/delayedDiscounting/delayedDiscountTurkv3.html"), `text/html`)
//    } ~
//    path("delayeddiscountv4") {
//      getFromFile(new File("views/delayedDiscounting/delayedDiscountv4.html"), `text/html`)
//    } ~
//    path("delayeddiscountturkv4") {
//      getFromFile(new File("views/delayedDiscounting/delayedDiscountTurkv4.html"), `text/html`)
//    } ~
    path("delayLeft") {
      getFromFile(new File("views/video/delayLeft.html"), `text/html`)
    } ~
    path("delayLeftVideo") {
      getFromFile(new File("views/video/delay_LEFT.mp4"), `video/mp4`)
    } ~
    path("delayRight") {
      getFromFile(new File("views/video/delayRight.html"), `text/html`)
    } ~
    path("delayRightVideo") {
      getFromFile(new File("views/video/delay_RIGHT.mp4"), `video/mp4`)
    } ~
    path("launchLeft") {
      getFromFile(new File("views/video/launchLeft.html"), `text/html`)
    } ~
    path("launchLeftVideo") {
      getFromFile(new File("views/video/launch_LEFT.mp4"), `video/mp4`)
    } ~
    path("launchRight") {
      getFromFile(new File("views/video/launchRight.html"), `text/html`)
    } ~
    path("launchRightVideo") {
      getFromFile(new File("views/video/launch_RIGHT.mp4"), `video/mp4`)
    } ~
     path("overlapLaunchLeft") {
      getFromFile(new File("views/video/overlapLaunchLeft.html"), `text/html`)
    } ~
    path("overlapLaunchLeftVideo") {
      getFromFile(new File("views/video/overlap_launch_LEFT.mp4"), `video/mp4`)
    } ~
    path("overlapLaunchRight") {
      getFromFile(new File("views/video/overlapLaunchRight.html"), `text/html`)
    } ~
    path("overlapLaunchRightVideo") {
      getFromFile(new File("views/video/overlap_launch_RIGHT.mp4"), `video/mp4`)
    } ~
    path("passLaunchLeft") {
      getFromFile(new File("views/video/passLaunchLeft.html"), `text/html`)
    } ~
    path("passLaunchLeftVideo") {
      getFromFile(new File("views/video/pass_launch_LEFT.mp4"), `video/mp4`)
    } ~
     path("passLaunchRight") {
      getFromFile(new File("views/video/passLaunchRight.html"), `text/html`)
    } ~
    path("passLaunchRightVideo") {
      getFromFile(new File("views/video/pass_launch_RIGHT.mp4"), `video/mp4`)
    } ~
     path("passLeft") {
      getFromFile(new File("views/video/passLeft.html"), `text/html`)
    } ~
    path("passLeftVideo") {
      getFromFile(new File("views/video/pass_LEFT.mp4"), `video/mp4`)
    } ~
    path("passRightVideo") {
      getFromFile(new File("views/video/pass_RIGHT.mp4"), `video/mp4`)
    } ~
    path("overlapLeftVideo") {
      getFromFile(new File("views/video/overlap_LEFT.mp4"), `video/mp4`)
    } ~
     path("overlapRightVideo") {
      getFromFile(new File("views/video/overlap_RIGHT.mp4"), `video/mp4`)
    } ~
    path("catchMiddleVideo") {
      getFromFile(new File("views/video/catch_MIDDLE.mp4"), `video/mp4`)
    } ~
    path("catchRightVideo") {
      getFromFile(new File("views/video/catch_RIGHT.mp4"), `video/mp4`)
    } ~
    path("demoResult") {
      getFromFile(new File("views/demoResult.csv"), `text/csv`)
    } ~
    path("css" / Rest) {fileName =>
      compressResponse(Gzip) {
        getFromFile(new File("views/css/"+fileName), `text/css`)
      }
    } ~
    path("js" / Rest) {fileName =>
      compressResponse(Gzip) {
        getFromFile(new File("views/js/" + fileName), `application/javascript`)
      }
    } ~
    path("img" / Rest) {fileName =>
      getFromFile(new File("views/img/"+fileName))
    } ~ complete(NotFound)
}
