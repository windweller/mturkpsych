package com.mturk.models

import scala.slick.driver.PostgresDriver.simple._

/**
 * Created by Aimingnie on 6/29/14.
 */
object Company {
  case class Company(id: Option[Int], s3FileLoc: Option[String], casperFileLoc: Option[String],
                     riskFactor: Option[String], managementDisc: Option[String], finStateSuppData: Option[String])

  class CompanyTable(tag: Tag) extends Table[Company](tag, "Company") {

    def id = column[Option[Int]]("COMP_ID", O.PrimaryKey, O.AutoInc)
    def s3FileLoc = column[Option[String]]("COMP_S3_FILE_LOC")
    def casperFileLoc = column[Option[String]]("COMP_CASPER_FILE_LOC")
    def riskFactor = column[Option[String]]("COMP_CASPER_FILE_LOC") //Item 1A
    def managementDisc = column[Option[String]]("COMP_MANAGE_DISC") //Item 7. Management's Discussion and Analysis of Financial Condition and Results of Operations
    def finStateSuppData = column[Option[String]]("COMP_FIN_STATEMENT") //Item 8. Financial Statements and Supplementary Data

    def * = (id, s3FileLoc, casperFileLoc, riskFactor, managementDisc, finStateSuppData) <> (Company.tupled, Company.unapply _)
  }

  val companies = TableQuery[CompanyTable]

  /* Insert a new Company  */
  /* takes in an Company, return the Company with auto id attached  */
  def insert(company: Company)(implicit s: Session): Company = {
    val companyId = companies returning companies.map(_.id) += company
    company.copy(id = companyId)
  }
}