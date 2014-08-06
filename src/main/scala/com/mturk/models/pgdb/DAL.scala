package com.mturk.models.pgdb

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
      else if (MTable.getTables("").list().isEmpty) {
        MTurker.mTurkers.ddl.create
      }
    }

    //Scan the specific folder and store file info on database
    //Will only work in server enviornment
    val filePath = "/root/experiments/mturk-company/docs/ProcessedSEC10KFiles"

//    val files = readFileNames()
//
//    def readFileNames():Try[List[Path]] = {
//      val p = Paths.get(filePath)
//      val stream = Try(Files.newDirectoryStream(p))
//      stream.map[List[Path]](ds =>
//        while(ds.iterator().hasNext){
//          ds.iterator().next()
//        }
//      )
//      for {
//        stream <- Try(Files.newDirectoryStream(p))
//        file:Path <- stream.iterator()
//      } yield file.getFileName
//    }
  }
}