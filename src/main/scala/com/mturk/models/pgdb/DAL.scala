package com.mturk.models.pgdb

import com.mturk.Config._

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.meta.MTable


object DAL {

  val db = Database.forURL(url = dbURL, user = dbUser, password= dbPassword, driver = dbDriver)

  def databaseInit() {
    db.withSession{ implicit session =>
      if (MTable.getTables("Company").list().isEmpty) {
        Company.companies.ddl.create
      } else if (MTable.getTables("User").list().isEmpty) {
        User.users.ddl.create
      }
    }
  }
}