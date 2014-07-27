package com.mturk.tasks.SECcompany


import akka.actor._
import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport
import spray.routing.Directives

import scala.concurrent.ExecutionContext.Implicits.global
import com.mturk.tasks.Util._


trait customRoutes {
  import org.json4s.JsonAST.JObject
  import spray.routing.Route
  import Directives._
  import com.mturk.tasks.SECcompany.SECCompanyProtocol._
  import akka.pattern.ask
  import spray.http.StatusCodes._
  import akka.util.Timeout
  import scala.concurrent.duration._
  import JsonImplicits._

  implicit val timeout = Timeout(5 seconds)

  def doPost(messageConstructor: JObject => AnyRef,
             authInfo: AuthInfo, secCompanyActor: ActorRef): Route = {
    entity(as[JObject]) { jObject =>
      if (authInfo.rejectedOrNot) {
        val response = (secCompanyActor ? messageConstructor(jObject)) //this constructs message
          .mapTo[TransOk]
          .map(result => result.succeedOrNot match {
          case true => (OK, "transaction successful")
          case false => (BadRequest, result.errorMessage)
        }).recover { case _ => (BadRequest, "An error has occurred! We will fix this")}
        complete(response)
      } else {
        complete(Unauthorized, "Please use HTTP header to authorize this command.")
      }
    }
  }
}

class SECCompanyService(secCompanyActor: ActorRef)(implicit system: ActorSystem)
  extends Directives with Authenticator with customRoutes {

  import akka.pattern.ask
  import com.mturk.tasks.SECcompany.SECCompanyProtocol._
  import spray.http.StatusCodes._
  import JsonImplicits._

  lazy val route =
    pathPrefix("sec") {
      path("help") {
        hostName { hn=>
          val localhostPortConfig = if (hn == "127.0.0.1") ":8080" else ""

          val routeApis = Map[String, (String, String)](
            "casperUpload" -> ("POST", "http://"+  hn + localhostPortConfig  + "/company/casper"),
            "webUpload" -> ("POST", "http://"+  hn + localhostPortConfig  + "/company/web"),
            "webOneCompany" -> ("GET","http://"+  hn + localhostPortConfig  + "/company/web")
          )
          complete(routeApis)
        }
      } ~
      pathPrefix("company") {
        pathPrefix("casper") {
          //http -a username:123 POST http://127.0.0.1:8080/sec/company/casper hello="23"
          authenticate(basicUserAuthenticator) { authInfo =>
            post {
                  doPost(JObjectFromCasper.apply, authInfo, secCompanyActor)
              }
            }
          } ~
          pathPrefix("web") {
            authenticate(basicUserAuthenticator) { authInfo =>
              post {
              //POST method comes in when one person completes the task
                doPost(JObjectFromWeb.apply, authInfo, secCompanyActor)
              } ~
              put {
              //PUT method comes in when one person can't complete the request, need
              //to retrieve a different document
                complete {
                  doPost(JObjectFromWebPUT.apply, authInfo, secCompanyActor)
                }
              }
            } ~
            get {
              val response = (secCompanyActor ? WebGetOneCompany).mapTo[TransOk]
                .map(result => result.succeedOrNot match {
                case true => (OK, result.company.get)
                case false => (BadRequest, result.errorMessage) //no more company left
              })
              complete(response)
            }
          }
        } ~
      pathPrefix("companies") {
        pathPrefix("casper") {
          get {
            val response = (secCompanyActor ? CasperGetAllCompanies)
            .mapTo[TransAllOk].map(result => result.succeedOrNot match {
              case true => (OK, result.companies.get)
              case false => (BadRequest, result.errorMessage)
            }).recover { case _ => (BadRequest, "An error has occurred! We will fix this")}
            complete(response)
          }
        }
      }
    }
}