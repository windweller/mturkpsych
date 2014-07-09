package com.mturk.models.pgdb

import java.sql.Timestamp
import com.github.nscala_time.time.Imports._
import scala.slick.driver.PostgresDriver.simple._

object Company {
  case class Company(id: Option[Int], s3FileLoc: Option[String], casperFileLoc: Option[String],
                     riskFactor: Option[String], managementDisc: Option[String],
                     finStateSuppData: Option[String], isRetrieved: Boolean, retrievedTime: Option[Timestamp])

  class CompanyTable(tag: Tag) extends Table[Company](tag, "Company") {

    def id = column[Option[Int]]("COMP_ID", O.PrimaryKey, O.AutoInc)
    def s3FileLoc = column[Option[String]]("COMP_S3_FILE_LOC")
    def casperFileLoc = column[Option[String]]("COMP_CASPER_FILE_LOC")
    def riskFactor = column[Option[String]]("COMP_RISK_FACTOR") //Item 1A
    def managementDisc = column[Option[String]]("COMP_MANAGE_DISC") //Item 7. Management's Discussion and Analysis of Financial Condition and Results of Operations
    def finStateSuppData = column[Option[String]]("COMP_FIN_STATEMENT") //Item 8. Financial Statements and Supplementary Data
    def isRetrieved = column[Boolean]("COMP_IS_RETTIEVED")
    def retrievedTime = column[Option[Timestamp]]("COMP_RETRIEVED_TIME")

    def * = (id, s3FileLoc, casperFileLoc, riskFactor, managementDisc,
      finStateSuppData, isRetrieved, retrievedTime) <> (Company.tupled, Company.unapply _)
  }

  val companies = TableQuery[CompanyTable]

  /* Insert a new Company  */
  /* takes in an Company, return the Company with auto id attached  */
  def insert(company: Company)(implicit s: Session): Company = {
    val companyId = companies returning companies.map(_.id) += company
    company.copy(id = companyId)
  }

  def insertCasper(casperFileLoc: String)(implicit s: Session): (Option[Company], Boolean, Option[String]) = {
    val companyQuery = for (c <- companies if c.casperFileLoc === casperFileLoc) yield c

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
          val company = Company(None, Some(s3Loc.get), Some(casperFileLoc), None, None, None, isRetrieved = false, None)
          val companyId = companies returning companies.map(_.id) += company
          (Some(company.copy(id = companyId)), true, None)
      }
    } else {
      (None, false, Some("One company file sharing the same casperFileLoc value already exists"))
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
        val updateCompany = company.copy(riskFactor= Some(riskFactor), managementDisc = Some(managementDisc),
        finStateSuppData = Some(finStateSuppData))
        q2.update(updateCompany)
        (Some(updateCompany), true, None)
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
   * @param s
   * @return one Company object is returned
   */
  def getOneCompany()(implicit s: Session): (Option[Company], Boolean, Option[String]) = {
    val companyFirstQuery = for (c <- companies if c.isRetrieved === false) yield c
    val firstBatchCompanies = companyFirstQuery.list()
    if (firstBatchCompanies.isEmpty) {

      val companySecondQuery = for (c <- companies if c.riskFactor.isNull
        && c.managementDisc.isNull && c.finStateSuppData.isNull) yield c

      val secondBatchCompanies = companySecondQuery.sortBy(_.retrievedTime.asc).list()
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
    val q = for (c <- companies if c.id === company.id) yield (c.isRetrieved, c.retrievedTime)
    val time = new Timestamp(DateTime.now.getMillis)
    q.update(true, Some(time))
    company.copy(retrievedTime = Some(time), isRetrieved = true)
  }

}