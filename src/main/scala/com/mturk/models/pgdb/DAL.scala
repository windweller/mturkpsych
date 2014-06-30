package com.mturk.models.pgdb

//import scala.slick.driver.MySQLDriver.simple._

import com.mturk.Config._
import com.mturk.models.Company

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.meta.MTable


object DAL {

  //either db.mysql._
  //or db.postgresql._

  val db = Database.forURL(url = dbURL, user = dbUser, password= dbPassword, driver = dbDriver)

  def databaseInit() {
    db.withSession{ implicit session =>
      if (MTable.getTables("Company").list().isEmpty) {
        Company.companies.ddl.create
      }
    }
  }
}