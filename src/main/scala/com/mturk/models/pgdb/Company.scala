package com.mturk.models.pgdb

import java.sql.Timestamp
import com.github.nscala_time.time.Imports._
import scala.slick.driver.PostgresDriver.simple._
import scala.util.Try

object Company {
  case class Company(id: Option[Int], txtURL: Option[String], htmlURL: Option[String], localFileLoc: Option[String],
                     riskFactor: Option[String], managementDisc: Option[String],
                     finStateSuppData: Option[String], unableToCompleteCount: Option[Int],
                     isRetrieved: Boolean, retrievedTime: Option[Timestamp], mturkID: Option[String])

  class CompanyTable(tag: Tag) extends Table[Company](tag, "Company") {

    def id = column[Option[Int]]("COMP_ID", O.PrimaryKey, O.AutoInc)
    def txtURL = column[Option[String]]("COMP_TXT_URL")
    def htmlURL = column[Option[String]]("COMP_HTML_URL")
    def localFileLoc = column[Option[String]]("COMP_LOCAL_FILE_LOC")
    def riskFactor = column[Option[String]]("COMP_RISK_FACTOR", O.DBType("TEXT")) //Item 1A
    def managementDisc = column[Option[String]]("COMP_MANAGE_DISC", O.DBType("TEXT")) //Item 7. Management's Discussion and Analysis of Financial Condition and Results of Operations
    def finStateSuppData = column[Option[String]]("COMP_FIN_STATEMENT", O.DBType("TEXT")) //Item 8. Financial Statements and Supplementary Data
    def unableToCompleteCount = column[Option[Int]]("COMP_UNABLE_TO_COMPLETE")
    def isRetrieved = column[Boolean]("COMP_IS_RETTIEVED")
    def retrievedTime = column[Option[Timestamp]]("COMP_RETRIEVED_TIME")
    def mturkID = column[Option[String]]("COMP_MTURK_ID")

    def * = (id, txtURL, htmlURL, localFileLoc, riskFactor, managementDisc,
      finStateSuppData, unableToCompleteCount, isRetrieved, retrievedTime, mturkID) <> (Company.tupled, Company.unapply _)
  }

  val companies = TableQuery[CompanyTable]

  /* Insert a new Company  */
  /* takes in an Company, return the Company with auto id attached  */
  def insert(company: Company)(implicit s: Session): Company = {
    val companyId = companies returning companies.map(_.id) += company
    company.copy(id = companyId)
  }

  /**
   * Not actually in use
   * @deprecated
   * @param casperFileLoc
   * @param s
   * @return
   */
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
          val company = Company(None, Some(s3Loc.get), None, Some(casperFileLoc), None, None, None, Some(0), isRetrieved = false, None, None)
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

      val company = Company(None, Some("/sec/company/file/txt/"+fileURL.get), Some("/sec/company/file/html/"+fileURL.get),
        Some(localFileLoc), None, None, None, Some(0), isRetrieved = false, None, None)
      val companyId = companies returning companies.map(_.id) += company
      company.copy(id = companyId)
    }else{
      throw new Exception("a file sharing the same localFileLoc value already exists")
    }
  }

  def updateFromWebCompanyRiskF(riskFactor: String, companyId: Int, mturkID: String)(implicit s: Session): Try[Company] = {
    val q = for (c <- companies if c.id === companyId) yield c
    q.list() match {
      case Nil => throw new Exception("CompanyId is wrong, the company is empty")
      case company::listTail =>
        Try {
          q.update(company.copy(riskFactor = Some(riskFactor), mturkID = Some(mturkID)))
          company.copy(riskFactor = Some(riskFactor), mturkID = Some(mturkID))
        }
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

//  def getAllCompanies()(implicit s: Session) = {
//    val companyQuery = for (c <- companies) yield c
//    companyQuery.list()
//  }

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
      val companySecondQuery = for (c <- companies if c.riskFactor.isNull && c.unableToCompleteCount <= 2) yield c

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
        //the old company is updated with count + 1
        //now retrieve a different company for user and js will update the link
        //and send altertify message
        getOneCompany()
    }
  }

}