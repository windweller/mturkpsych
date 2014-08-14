package com.mturk.models.pgdb

import java.sql.Timestamp
import akka.actor.ActorLogging
import com.github.nscala_time.time.Imports._
import scala.slick.driver.PostgresDriver.simple._

object Company {
  case class Company(id: Option[Int], txtURL: Option[String], htmlURL: Option[String], localFileLoc: Option[String],
                     riskFactor: Option[String], managementDisc: Option[String],
                     finStateSuppData: Option[String], unableToCompleteCount: Option[Int],
                     isRetrieved: Boolean, retrievedTime: Option[Timestamp])

  class CompanyTable(tag: Tag) extends Table[Company](tag, "Company") {

    def id = column[Option[Int]]("COMP_ID", O.PrimaryKey, O.AutoInc)
    def txtURL = column[Option[String]]("COMP_TXT_URL")
    def htmlURL = column[Option[String]]("COMP_HTML_URL")
    def localFileLoc = column[Option[String]]("COMP_LOCAL_FILE_LOC")
    def riskFactor = column[Option[String]]("COMP_RISK_FACTOR") //Item 1A
    def managementDisc = column[Option[String]]("COMP_MANAGE_DISC") //Item 7. Management's Discussion and Analysis of Financial Condition and Results of Operations
    def finStateSuppData = column[Option[String]]("COMP_FIN_STATEMENT") //Item 8. Financial Statements and Supplementary Data
    def unableToCompleteCount = column[Option[Int]]("COMP_UNABLE_TO_COMPLETE")
    def isRetrieved = column[Boolean]("COMP_IS_RETTIEVED")
    def retrievedTime = column[Option[Timestamp]]("COMP_RETRIEVED_TIME")

    def * = (id, txtURL, htmlURL, localFileLoc, riskFactor, managementDisc,
      finStateSuppData, unableToCompleteCount, isRetrieved, retrievedTime) <> (Company.tupled, Company.unapply _)
  }

  val companies = TableQuery[CompanyTable]

  /* Insert a new Company  */
  /* takes in an Company, return the Company with auto id attached  */
  def insert(company: Company)(implicit s: Session): Company = {
    val companyId = companies returning companies.map(_.id) += company
    company.copy(id = companyId)
  }

  /* Not in actual use */
  def insertCasper(casperFileLoc: String)(implicit s: Session): (Option[Company], Boolean, Option[String]) = {
    val companyQuery = for (c <- companies if c.localFileLoc === casperFileLoc) yield c

    if (companyQuery.list().isEmpty) {
      /*
      Input should look like:
      E:\Allen\SECProject\CompanyWithStockTrend\Processed2012QTR1\0000930413-12-001949_finalDoc.html
      Output should look like:
      Option[String] = Some(Processed2012QTR1\0000930413-12-001949_finalDoc.html)
      */
      val pattern = "(?:Processed)(2012.+$)".r
      val s3Loc = pattern.findFirstIn(casperFileLoc)

      s3Loc.isEmpty match {
        case true => (None, false, Some("casperFileLoc value doesn't fit format and the server can't extract certain value"))
        case false =>
          val company = Company(None, Some(s3Loc.get), None, Some(casperFileLoc), None, None, None, Some(0), isRetrieved = false, None)
          val companyId = companies returning companies.map(_.id) += company
          (Some(company.copy(id = companyId)), true, None)
      }
    } else {
      (None, false, Some("One company file sharing the same casperFileLoc value already exists"))
    }
  }

  //Use Try[Company] to capture this
  def insertCompany(fileURL: Option[String], localFileLoc: String)(implicit s: Session): Company = {

    if (fileURL.isEmpty) throw new Exception("URL is empty")

    val companyQuery = for (c <- companies if c.localFileLoc === localFileLoc) yield c
    if (companyQuery.list().isEmpty) {
      val rootURL = "http://mturk-company.mindandlanguagelab.com/company/file/"
      val company = Company(None, Some(rootURL+"txt/"+fileURL.get), Some(rootURL + "html/"+fileURL.get),
        Some(localFileLoc), None, None, None, Some(0), isRetrieved = false, None)
      val companyId = companies returning companies.map(_.id) += company
      company.copy(id = companyId)
    }else{
      throw new Exception("a file sharing the same localFileLoc value already exists")
    }
  }

  def updateFromWeb(riskFactor: String, managementDisc: String,
                    finStateSuppData: String, companyId: Int)(implicit s: Session):
                    (Option[Company], Boolean, Option[String]) = {
    val q = for (c <- companies if c.id === companyId) yield c
    q.list() match {
      case Nil => (None, false, Some("companyId is wrong, can't find the company with that id."))
      case list: List[Company] =>
        val company = list.head
        val q2 = for (c <- companies if c.id === company.id) yield c
        val updatedCompany = company.copy(riskFactor= Some(riskFactor), managementDisc = Some(managementDisc),
          finStateSuppData = Some(finStateSuppData))
        q2.update(updatedCompany)
        (Some(updatedCompany), true, None)
    }
  }

  def getAllCompanies()(implicit s: Session) = {
    val companyQuery = for (c <- companies) yield c
    companyQuery.list()
  }

  /**
   * Used by web client to retrieve one
   * random company that is not completed yet
   * It checks two things: isRetrieved mark false or true
   * If there is no previously unretrieved company document
   * It ranks all documents with empty results by time desc
   * order, and return the oldest one (only if it is longer
   * than 1 hr)
   *
   * Also it will not retrieve companies that have more than
   * 2 counts of unable to complete
   *
   * @param s
   * @return one Company object is returned
   */
  def getOneCompany()(implicit s: Session): (Option[Company], Boolean, Option[String]) = {
    //first query: unretrieved
    val companyFirstQuery = for (c <- companies
         if c.isRetrieved === false && c.unableToCompleteCount <= 2) yield c

    val firstBatchCompanies = companyFirstQuery.list()
    if (firstBatchCompanies.isEmpty) {
      //second query: null ones and unable to complete <= 2
      //first step, mark those qualifying companies' "isRetrieved" = False
      val companySecondQuery = for (c <- companies if c.riskFactor.isNull
        && c.managementDisc.isNull && c.finStateSuppData.isNull && c.unableToCompleteCount <= 2) yield c

      val secondBatchCompanies = companySecondQuery.sortBy(_.retrievedTime.asc).list()  //start from earliest record
      if (secondBatchCompanies.isEmpty) {
        (None, false, Some("There is no document fitting requirements left"))
      } else {
        getFirstCompanyFromBatch(secondBatchCompanies)
      }
    } else {
      getFirstCompanyFromBatch(firstBatchCompanies)
    }
  }

  private def getFirstCompanyFromBatch(batch: List[Company])(implicit s: Session) = {
    val companyResult = batch.head
    val result = updateOnRetrieval(companyResult)
    (Some(result), true, None)
  }

  def updateOnRetrieval(company: Company)(implicit s: Session) = {
    println("company passed in for update: "+ company.id)
    val q = for (c <- companies if c.id === company.id) yield (c.isRetrieved, c.retrievedTime)
    val time = new Timestamp(DateTime.now.getMillis)
    q.update(true, Some(time))
    company.copy(retrievedTime = Some(time), isRetrieved = true)
  }

  def addUnableToComplete(companyId: Int)(implicit s: Session): (Option[Company], Boolean, Option[String]) = {
    val q = for (c <- companies if c.id === companyId) yield c
    q.list() match {
      case Nil => (None, false, Some("companyId is wrong, can't find the company with that id."))
      case list: List[Company] =>
        val unableToCompCount = list.head.unableToCompleteCount
        val updatedCompany = list.head.copy(unableToCompleteCount = Some(unableToCompCount.get + 1))
        q.update(updatedCompany)
        (Some(updatedCompany), true, None)
    }
  }

}