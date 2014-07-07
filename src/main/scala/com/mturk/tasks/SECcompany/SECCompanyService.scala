package com.mturk.tasks.SECcompany

import java.sql.Timestamp

import akka.actor._
import com.mturk.models.pgdb._
import org.json4s.JsonAST.JObject
import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport
import spray.routing.Directives
import spray.routing.authentication._
import spray.routing.directives.AuthMagnet

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


object JsonImplicits extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats
}

case class AuthInfo(username: Option[String], rejectedOrNot: Boolean)

trait Authenticator {

  def getUser(username: String, token: String): Option[User.User] = {
    import com.github.nscala_time.time.Imports._
    //TODO: get from DB
    if (token == "123" && username == "username")
      Some(User.User(None, Some("goodUser"), "", "", "", new Timestamp(DateTime.now.getMillis), 1, None))
    else
      None
  }

  def basicUserAuthenticator(implicit ec: ExecutionContext): AuthMagnet[AuthInfo] = {
    def validateUser(userPass: Option[UserPass]): Option[AuthInfo] = {
      val user = userPass.map[Option[User.User]](u => getUser(u.user, u.pass))
      user match {
        case Some(Some(u)) => Some(AuthInfo(u.name, rejectedOrNot = true))
        case Some(None) => Some(AuthInfo(None, rejectedOrNot = false))
        case None => Some(AuthInfo(None, rejectedOrNot = false))
      }
    }

    def authenticator(userPass: Option[UserPass]): Future[Option[AuthInfo]] = Future {
      validateUser(userPass)
    }

    BasicAuth(authenticator _, realm = "Lab Authorized API")
  }
}

class SECCompanyService(secCompanyActor: ActorRef)(implicit system: ActorSystem)
  extends Directives with Authenticator {

  import akka.pattern.ask
  import akka.util.Timeout
  import com.mturk.tasks.SECcompany.JsonImplicits._
  import com.mturk.tasks.SECcompany.SECCompanyProtocol._
  import spray.http.StatusCodes._
  import scala.concurrent.duration._

  implicit val timeout = Timeout(5 seconds)

  lazy val route =
    pathPrefix("sec") {
      pathEnd {
        complete("Welcome to Dr. Wolff's online lab :)")
      } ~
      pathPrefix("company") {
        pathPrefix("casper") {
          authenticate(basicUserAuthenticator) { authInfo =>
            post {
              entity(as[JObject]) { company: JObject =>
                if (authInfo.rejectedOrNot) {
                  val response = (secCompanyActor ? jObjectFromCasper(company))
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
              } ~
              put {
                complete {
                  "To be implemented."
                }
              }
            }
          } ~
          pathPrefix("web") {
            authenticate(basicUserAuthenticator) { AuthInfo =>
              post {
                entity(as[JObject]) { company =>
                  if (AuthInfo.rejectedOrNot) {
                    val response = (secCompanyActor ? jObjectFromWeb(company))
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
              } ~
              put {
                complete {
                  "To be implemented."
                }
              }
            }
            get {
              val response = (secCompanyActor ? WebGetOneCompany).mapTo[TransOk]
                .map(result => result.succeedOrNot match {
                case true => (OK, result.company.get)
                case false => (BadRequest, result.errorMessage)
              })
              complete(response)
            }
          }
        }~
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

