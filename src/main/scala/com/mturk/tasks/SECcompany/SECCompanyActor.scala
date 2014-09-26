package com.mturk.tasks.SECcompany

import akka.actor.{ActorLogging, Actor}
import com.mturk.tasks.Util.AuthInfo
import org.json4s.JsonAST.JObject
import com.mturk.models.pgdb._

import scala.util.{Try, Success}


class SECCompanyActor extends Actor with ActorLogging {
  import com.mturk.tasks.SECcompany.SECCompanyProtocol._
  import com.mturk.tasks.Util._

  def receive = {

    case JObjectFromCasper(jObject, authInfo) =>
      if (jObject.isDefined("casperFileLoc")) {
        val casperFileLoc = jObject.getValue("casperFileLoc")
        val result = DAL.db.withSession{ implicit session =>
          Company.insertCasper(casperFileLoc.toString) //careful with tostring method
        }
        sender ! TransOk(result._1, result._2, result._3)
      }else{
        sender ! TransOk(None, succeedOrNot= false, Some("Can't find casperFileLoc value in JSON"))
      }

    //&& jObject.isDefined("managementDisc") && jObject.isDefined("finStateSuppData")
    case JObjectFromWeb(jObject, authInfo) =>
      if (jObject.isDefined("riskFactor")  && jObject.isDefined("companyId") && jObject.isDefined("mturkID")) {

        jObject.getValueInt("companyId") match {
          case None => sender ! TransOk(None, succeedOrNot = false, Some("companyId is not a valid number"))
          case Some(companyId) =>
            val result = DAL.db.withSession { implicit session =>
              Company.updateFromWebCompanyRiskF(jObject.getValue("riskFactor"),
                jObject.getValue("companyId").toInt, jObject.getValue("mturkID"))
            }
//            result.map(r => sender ! TransOk(Some(r), true, None))
            sender ! TryCompany(result)
        }
      }else{
        sender ! TransOk(None, succeedOrNot = false, Some("At least one of four " +
          "fields: riskFactor, companyId, mturkID was not defined in JSON"))
      }

    case JObjectFromWebPUT(jObject, authInfo) =>
       if (jObject.isDefined("companyId")) {
         jObject.getValueInt("companyId") match {
           case None => sender ! TransOk(None, succeedOrNot = false, Some("companyId is not a valid number"))
           case Some(companyId) =>
             val result = DAL.db.withSession { implicit session =>
               Company.addUnableToComplete(companyId)
             }
             sender ! TransOk(result._1, result._2, result._3)
         }
       }

    case WebGetOneCompany =>
      val result = DAL.db.withSession{ implicit session =>
        Company.getOneCompany()
      }
      sender ! TransOk(result._1, result._2, result._3)

    case CasperGetAllCompanies =>
      DAL.db.withSession{ implicit  session =>
        Company.getAllCompanies() match {
          case Nil => sender ! TransAllOk(None, succeedOrNot = false, Some("There is no row in the company table"))
          case companies: List[Company.Company] =>
            sender ! TransAllOk(Some(companies), succeedOrNot = true, None)
        }
      }
  }
}

object SECCompanyProtocol {
  case class JObjectFromCasper(jObject: JObject, authInfo: AuthInfo)
  case class JObjectFromWeb(jObject: JObject, authInfo: AuthInfo)
  case class JObjectFromWebPUT(jObject: JObject, authInfo: AuthInfo)
  case class TryCompany(company: Try[Company.Company])
  case class TransOk(company: Option[Company.Company], succeedOrNot: Boolean, errorMessage: Option[String])
  case class TransAllOk(companies: Option[List[Company.Company]], succeedOrNot: Boolean, errorMessage: Option[String])

  case object WebGetOneCompany
  case object CasperGetAllCompanies

  case class Trial(name: String)
  val test = Trial("hello!")
  test.hashCode()

}