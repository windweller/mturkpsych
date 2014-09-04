package com.mturk.api

import akka.actor.{Props, ActorSystem}
import akka.event.Logging.InfoLevel
import com.mturk.tasks.delayedDiscounting.DelayedDiscountService
import com.mturk.tasks.mTurkerProgress.MTurkerProgressService
import com.mturk.tasks.triadTest.TriadTestService
import spray.http.HttpRequest
import spray.http.StatusCodes.{MovedPermanently, NotFound }
import spray.httpx.encoding.Gzip
import spray.routing.{Directives, RouteConcatenation}
import spray.routing.directives.LogEntry
import java.io.File
import spray.http.MediaTypes._
import com.mturk.tasks.SECcompany._

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
    new SECCompanyService(companyActor).route ~
    new MTurkerProgressService(mTurkerActor).route ~
    new TriadTestService(triadActor).route ~
    new DelayedDiscountService(delayedDiscountActor).route ~
    staticRoute
  }

  private def showReq(req : HttpRequest) = LogEntry(req.uri, InfoLevel)
}

trait StaticRoute extends Directives {
  this: AbstractSystem =>

  lazy val staticRoute =
    pathEndOrSingleSlash {
        getFromFile(new File("views/home.html"), `text/html`)
    } ~
    path("sec") {
      getFromFile(new File("views/sec.html"), `text/html`)
    } ~
    path("secturk") {
      getFromFile(new File("views/secturk.html"), `text/html`)
    } ~
    path("triad") {
      getFromFile(new File("views/triad.html"), `text/html`)
    } ~
    path("triadturk") {
      getFromFile(new File("views/triadturk.html"), `text/html`)
    } ~
    path("delayeddiscountv1") {
      getFromFile(new File("views/delayedDiscountv1.html"), `text/html`)
    } ~
    path("delayeddiscountturkv1") {
      getFromFile(new File("views/delayedDiscountTurkv1.html"), `text/html`)
    } ~
    path("delayeddiscountv2") {
      getFromFile(new File("views/delayedDiscountv2.html"), `text/html`)
    } ~
    path("delayeddiscountturkv2") {
      getFromFile(new File("views/delayedDiscountTurkv2.html"), `text/html`)
    } ~
    path("css" / Segment) {fileName =>
      compressResponse(Gzip) {
        getFromFile(new File("views/css/"+fileName), `text/css`)
      }
    } ~
    path("css" / Segment / Segment) {(subFolder, fileName) =>
      compressResponse(Gzip) {
        getFromFile(new File("views/css/" + subFolder + "/" + fileName), `text/css`)
      }
    } ~
    path("js" / Segment) {fileName =>
      compressResponse(Gzip) {
        getFromFile(new File("views/js/" + fileName), `application/javascript`)
      }
    } ~
    path("js" / Segment / Segment) { (subFolder, fileName) =>
      compressResponse(Gzip) {
        getFromFile(new File("views/js/" + subFolder + "/" + fileName), `application/javascript`)
      }
    } ~
    path("img" / Segment) {fileName =>
      getFromFile(new File("views/img/"+fileName))
    } ~ complete(NotFound)
}