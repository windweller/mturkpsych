package com.mturk.models.pgdb


import akka.actor.{Props, ActorSystem}
import java.nio.file._
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
import scala.annotation.tailrec

import com.mturk.Config._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.meta.MTable


object DAL {

  val db = (dbUser, dbPassword) match {
    case (None, None) => Database.forURL(dbConnect, driver=dbDriver)
    case _ => Database.forURL(url = dbURL, user=dbUser.get, password=dbPassword.get, driver = dbDriver)
  }

  def databaseInit()(implicit system: ActorSystem) {

    db.withSession{ implicit session =>

      if (MTable.getTables("Company").list().isEmpty) {
        Company.companies.ddl.create

        /**
         * This is part of initialization of MTurk Table
         * We are saving all company files into this table
         * Scanning our server's directories
         */
        println(secFileLoc)
        val files = readFileNames(secFileLoc)
        saveToDataBase(files)
      }

      if (MTable.getTables("User").list().isEmpty) {
        User.users.ddl.create
      }

      if (MTable.getTables("MTurker").list().isEmpty) {
        MTurker.mTurkers.ddl.create
      }
    }
  }

  def readFileNames(filePath: String):Option[List[Path]] = {
    val p = Paths.get(filePath)
    val resultList = recursiveTraverse(ListBuffer[Path](p), ListBuffer[Path]())
    if(resultList.isEmpty) None else Some(resultList.toList)
  }

  @tailrec
  def recursiveTraverse(filePaths: ListBuffer[Path], resultFiles: ListBuffer[Path]): ListBuffer[Path] = {
    if (filePaths.isEmpty) resultFiles
    else {
      val head = filePaths.head
      val tail = filePaths.tail

      if (Files.isDirectory(head)) {
        val stream: Try[DirectoryStream[Path]] = Try(Files.newDirectoryStream(head))
        stream match {
          case Success(st) =>
            val iterator = st.iterator()
            while (iterator.hasNext) {
              tail += iterator.next()   //kidding me? This generates a new List not modifying old one
            }
          case Failure(ex) => println(s"The file path is incorrect: ${ex.getMessage}")
        }
        stream.map(ds => ds.close())
        recursiveTraverse(tail, resultFiles)
      }
      else{
        if (head.toString.contains(".html") || head.toString.contains(".txt")) {
          recursiveTraverse(tail, resultFiles += head)
        }else{
          recursiveTraverse(tail, resultFiles)
        }
      }
    }
  }

  def saveToDataBase(paths: Option[List[Path]])(implicit system: ActorSystem) {
    import com.mturk.tasks.timer.SECCompanyInitTimers
    import com.mturk.tasks.timer.SECCompanyInitTimersMsg._

    val timer = system.actorOf(Props(new SECCompanyInitTimers(paths.map(l => l.size))))

    db.withSession { implicit session =>
      for (ps <- paths; filepath <- ps) {
        val regex = "(2012.+)".r
        val url = regex.findFirstIn(filepath.toString)
        val company = Try(Company.insertCompany(url, filepath.toString)).recover{
          case e: Exception => println(e.getMessage)
        }
        timer ! InitAddOne
      }
    }
  }
}