package com.mturk.tasks

import com.mturk.models.pgdb.{MTurker, DAL}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.JsonAST.JObject
import spray.httpx.Json4sSupport

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

object Util {

  /**
   * Utility classes and methods
   */
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

  def extractJObjectEntity(fields: List[String], jObject: JObject): Either[String, List[String]] = {

    val result = for (field <- fields if jObject.isDefined(field)) yield jObject.getValue(field)

    if (result.isEmpty || result.size != fields.size) Left("fields: "+fields.diff(result)+" was not defined in JSON") else Right(result)
  }

  /**
   * It would be mturk id and mturk commToken
   * @param token
   * @return
   */
  def getUser(token: String): AuthInfo = {
    DAL.db.withSession { implicit session =>
      val mTurker = Try(MTurker.getByToken(token))
      mTurker match {
        case Success(result) =>
          if (result._2) AuthInfo(result._1, accepted = true)
          else AuthInfo(None, accepted=false)
        case Failure(ex) => println("Basic Authentication error: "+ex.getMessage); AuthInfo(None, accepted=false)
      }
    }
  }

  /**
   * @deprecated
   * Basic Auth authenticator used
   * by all other tasks
   * usage: authenticate(basicUserAuthenticator) { authInfo => }
   */

  case class AuthInfo(mturker: Option[MTurker.MTurker], accepted: Boolean)

  trait Authenticator {

    import spray.routing.authentication.{BasicAuth, UserPass}
    import spray.routing.directives.AuthMagnet
    import scala.concurrent.{Future, ExecutionContext}


//    def basicUserAuthenticator(implicit ec: ExecutionContext): AuthMagnet[AuthInfo] = {
//      def validateUser(userPass: Option[UserPass]): Option[AuthInfo] = {
//        val mturker = userPass.flatMap[MTurker.MTurker](u => getUser(u.user, u.pass))
//        mturker match {
//          case Some(m) => println("authen passed!");Some(AuthInfo(m.mturkId, rejectedOrNot = true))
//          case None => throw new Exception //Some(AuthInfo(None, rejectedOrNot = false))
//        }
//      }
//
//      def authenticator(userPass: Option[UserPass]): Future[Option[AuthInfo]] = Future {
//        validateUser(userPass)
//      }
//
//      BasicAuth(authenticator _, realm = "Lab Authorized API")
//    }
  }
}