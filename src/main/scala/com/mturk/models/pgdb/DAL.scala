package com.mturk.models.pgdb

import java.io.IOException
import java.nio.file._

import com.mturk.Config._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.meta.MTable
import scala.util.Try
import scala.collection.JavaConverters


object DAL {

  val db = (dbUser, dbPassword) match {
    case (None, None) => Database.forURL(dbConnect, driver=dbDriver)
    case _ => Database.forURL(url = dbURL, user=dbUser.get, password=dbPassword.get, driver = dbDriver)
  }

  def databaseInit() {
    db.withSession{ implicit session =>
      if (MTable.getTables("Company").list().isEmpty) {
        Company.companies.ddl.create
      }
      else if (MTable.getTables("User").list().isEmpty) {
        User.users.ddl.create
      }
      else if (MTable.getTables("MTurk").list().isEmpty) {
        MTurker.mTurkers.ddl.create
        /**
         * This is part of initialization of MTurk Table
         * We are saving all company files into this table
         * Scanning our server's directories
         */
        val filePath = "/root/experiments/mturk-company/docs/ProcessedSEC10KFiles"

        val files = readFileNames(filePath)
        saveToDataBase(files)
      }
    }
  }

  def readFileNames(filePath: String):Option[List[Path]] = {
    val p = Paths.get(filePath)
    val stream = Try(Files.newDirectoryStream(p))

    val listOfFiles = List[Path]()
    stream.map[Unit]{stream =>
      while (stream.iterator().hasNext) {
        listOfFiles :+ stream.iterator().next()
      }
    }
    if(listOfFiles.isEmpty) None else Some(listOfFiles)
  }

  def saveToDataBase(paths: Option[List[Path]]) {
    db.withSession { implicit session =>
      for (ps <- paths; filepath <- ps) {
        val regex = "(2012.+)".r
        val url = regex.findFirstIn(filepath.toString)
        val company = Try(Company.insertCompany(url, filepath.toString)).recover{
          case e: Exception => println(e.getMessage)
        }
      }
    }
  }
}