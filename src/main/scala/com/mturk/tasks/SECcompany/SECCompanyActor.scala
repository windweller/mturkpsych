package com.mturk.tasks.SECcompany

import akka.actor.{ActorLogging, Actor}
import org.json4s.JsonAST.JObject
import com.mturk.models.pgdb._


class SECCompanyActor extends Actor with ActorLogging {
  import com.mturk.tasks.SECcompany.SECCompanyProtocol._
  import com.mturk.tasks.Util._


  def receive = {

    case JObjectFromCasper(jObject) =>
      if (jObject.isDefined("casperFileLoc")) {
        val casperFileLoc = jObject.getValue("casperFileLoc")
        val result = DAL.db.withSession{ implicit session =>
          Company.insertCasper(casperFileLoc.toString) //careful with tostring method
        }
        sender ! TransOk(result._1, result._2, result._3)
      }else{
        sender ! TransOk(None, succeedOrNot= false, Some("Can't find casperFileLoc value in JSON"))
      }

    case JObjectFromWeb(jObject) =>
      if (jObject.isDefined("riskFactor") && jObject.isDefined("managementDisc")
        && jObject.isDefined("finStateSuppData") && jObject.isDefined("companyId")) {

        jObject.getValueInt("companyId") match {
          case None => sender ! TransOk(None, succeedOrNot = false, Some("companyId is not a valid number"))
          case Some(companyId) =>
            val result = DAL.db.withSession { implicit session =>
              Company.updateFromWeb(jObject.getValue("riskFactor"),
                jObject.getValue("managementDisc"), jObject.getValue("finStateSuppData"),
                jObject.getValue("companyId").toInt)
            }
            sender ! TransOk(result._1, result._2, result._3)
        }
      }else{
        sender ! TransOk(None, succeedOrNot = false, Some("At least one of four " +
          "fields: riskFactor, managementDisc, finStateSuppData, or companyId was not defined in JOSN"))
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
  case class JObjectFromCasper(jObject: JObject)
  case class TransOk(company: Option[Company.Company], succeedOrNot: Boolean, errorMessage: Option[String])
  case class JObjectFromWeb(jObject: JObject)
  case class TransAllOk(companies: Option[List[Company.Company]], succeedOrNot: Boolean, errorMessage: Option[String])

  case object WebGetOneCompany
  case object CasperGetAllCompanies
}