package com.mturk.tasks.SECcompany


import java.io.File

import akka.actor._
import com.mturk.tasks.Util._
import org.json4s.JsonAST.JObject
import spray.http.MediaTypes._
import spray.httpx.encoding.Gzip
import spray.routing.Directives

import scala.concurrent.ExecutionContext.Implicits.global


trait CustomRoutes {
  import Directives._
  import JsonImplicits._
  import akka.pattern.ask
  import akka.util.Timeout
  import com.mturk.tasks.SECcompany.SECCompanyProtocol._
  import org.json4s.JsonAST.JObject
  import spray.http.StatusCodes._
  import spray.routing.Route

  import scala.concurrent.duration._

  implicit val timeout = Timeout(5 seconds)

  /**
   * @deprecated
   * This method is depreciated due to the
   * philosophical difference it poses
   * use the doTryPost instead
   * @param messageConstructor
   * @param authInfo
   * @param secCompanyActor
   * @return
   */
  def doPost(messageConstructor: (JObject, AuthInfo) => AnyRef,
             authInfo: AuthInfo, secCompanyActor: ActorRef): Route = {
    entity(as[JObject]) { jObject =>
      if (authInfo.accepted) {
        val response = (secCompanyActor ? messageConstructor(jObject, authInfo)) //this constructs message
          .mapTo[TransOk]
          .map(result => result.succeedOrNot match {
          case true => (OK, "transaction successful")
          case false => (BadRequest, result.errorMessage)
        }).recover { case e => (BadRequest, e.getMessage)}
        complete(response)
      } else {
        complete(Unauthorized, "Please use HTTP header to authorize this command.")
      }
    }
  }

  /**
   * New Post method to parse the response as "Try[E]"
   * @param messageConstructor
   * @param authInfo
   * @param secCompanyActor
   * @return
   */
  def doTryPost(messageConstructor: (JObject, AuthInfo) => AnyRef,
                authInfo: AuthInfo, secCompanyActor: ActorRef): Route = {
    import scala.util.{Failure, Success}
    entity(as[JObject]) { jObject =>
      if (authInfo.accepted) {
        val response = (secCompanyActor ? messageConstructor(jObject, authInfo)) //this constructs message
          .mapTo[TryCompany]
          .map(r => r.company match {
          case Success(re) => (OK, "transaction successful")
          case Failure(e) => (BadRequest, e.getMessage)
        })
        complete(response)
      } else {
        complete(Unauthorized, "Please use HTTP header to authorize this command.")
      }
    }
  }
}

class SECCompanyService(secCompanyActor: ActorRef)(implicit system: ActorSystem)
  extends Directives with Authenticator with CustomRoutes {

  import JsonImplicits._
  import akka.pattern.ask
  import com.mturk.Config._
  import com.mturk.tasks.SECcompany.SECCompanyProtocol._
  import spray.http.StatusCodes._

  lazy val route =
    pathPrefix("sec") {
      path("help") {
        hostName { hn =>
          val localhostPortConfig = if (hn == "127.0.0.1") ":8080" else ""
          val httpPrefix = if (hn == "127.0.0.1") "http://" else "https://"

          val routeApis = Map[String, (String, String)](
            "casperUpload" -> ("POST", httpPrefix +  hn + localhostPortConfig  + "/sec/company/casper"),
            "webUpload" -> ("POST", httpPrefix +  hn + localhostPortConfig  + "/sec/company/web"),
            "webUnableToComp" -> ("PUT", httpPrefix +  hn + localhostPortConfig  + "/sec/company/web"),
            "webOneCompany" -> ("GET",httpPrefix +  hn + localhostPortConfig  + "/sec/company/web")
          )
          complete(routeApis)
        }
      } ~
      pathPrefix("company") {
        pathPrefix("casper") {
          //http -a username:123 POST http://127.0.0.1:8080/sec/company/casper hello="23"
            post {
              cookie("commToken") { commToken =>
                val authInfo = getUser(commToken.content)
                doPost(JObjectFromCasper.apply, authInfo, secCompanyActor)
              }
            }
          } ~
          pathPrefix("web") {
              post {
                entity(as[JObject]) { jObject =>
                  //POST method comes in when one person completes the task
                  optionalCookie("commToken") {
                    case None =>
                      complete(Unauthorized, "Please retrieve and add your communication token as cookie.")
                    case Some(commToken) =>
                      val authInfo = getUser(commToken.content)
                      doTryPost(JObjectFromWeb.apply, authInfo, secCompanyActor)
                  }
                }
              } ~
              put {
                entity(as[JObject]) { jObject =>
                  //PUT method comes in when one person can't complete the request, need
                  //to retrieve a different document returned
                  optionalCookie("commToken") {
                    case None =>
                      complete(Unauthorized, "Please retrieve and add your communication token as cookie.")
                    case Some(commToken) =>
                      val authInfo = getUser(commToken.content)
                      if (!authInfo.accepted) complete((Unauthorized, "Communication Token authentication not passed."))
                      val response = (secCompanyActor ? JObjectFromWebPUT(jObject, authInfo))
                        .mapTo[TransOk]
                        .map(result => result.succeedOrNot match {
                        case true => (OK, result.company.get)
                        case false => (Unauthorized, result.errorMessage)
                      })
                      complete(response)
                  }
                }
              } ~
              get {
                hostName { hn =>
                  complete {
                    val response = (secCompanyActor ? WebGetOneCompany).mapTo[TransOk]
                      .map(result => result.succeedOrNot match {
                      case true =>
                        val localhostPortConfig = if (hn == "127.0.0.1") ":8080" else ""
                        val newTxtURL = "http://" +  hn + localhostPortConfig  + result.company.get.txtURL.get
                        val newHtmlURL = "http://" +  hn + localhostPortConfig  + result.company.get.htmlURL.get
                        (OK, result.company.get.copy(txtURL = Some(newTxtURL), htmlURL = Some(newHtmlURL)))
                      case false => (BadRequest, result.errorMessage) //no more company left
                        //TODO: make sure the other side responds to error well
                    })
                  response
                  }
                }
              }
          } ~
          pathPrefix("file") {
            path("txt" / Rest) { fileName =>
              get {
                println(secFileLoc + fileName)
                compressResponse(Gzip) {
                  getFromFile(new File(secFileLoc + fileName), `application/octet-stream`)
                }
              }
            } ~
            path("html" / Rest) { fileName =>
              get {
                println(secFileLoc + fileName)
                compressResponse(Gzip) {
                  getFromFile(new File(secFileLoc + fileName), `text/html`)
                }
              }
            } ~ complete(NotFound)
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