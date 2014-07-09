package com.mturk.tasks

import org.json4s.{DefaultFormats, Formats}
import org.json4s.JsonAST.JObject
import spray.httpx.Json4sSupport

object Util {

  implicit class jObjectImprovements(val jo: JObject) {
    def isDefined(value: String) = jo.values.get(value).isDefined
    def getValue(value: String): String = jo.values.get(value).get.toString
    def getValueInt(value: String): Option[Int] = {
      import scala.util.control.Exception._
      val s = jo.values.get(value).get.toString
      catching(classOf[NumberFormatException]) opt s.toInt
    }

  }

  object JsonImplicits extends Json4sSupport {
    implicit def json4sFormats: Formats = DefaultFormats
  }

  case class AuthInfo(username: Option[String], rejectedOrNot: Boolean)

  /**
   * Basic Auth authenticator used
   * by all other tasks
   * usage: authenticate(basicUserAuthenticator) { authInfo => }
   */
  trait Authenticator {

    import java.sql.Timestamp
    import com.mturk.models.pgdb.User
    import spray.routing.authentication.{BasicAuth, UserPass}
    import spray.routing.directives.AuthMagnet
    import scala.concurrent.{Future, ExecutionContext}

    def getUser(username: String, token: String): Option[User.User] = {
      import com.github.nscala_time.time.Imports._
      //TODO: get from DB
      if (token == "26666" && username == "emory")
        Some(User.User(None, Some("goodUser"), "", "", Some(""), new Timestamp(DateTime.now.getMillis), 1))
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
}