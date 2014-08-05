package com.mturk

import akka.actor.{ActorSystem, Props}
import akka.io.{IO, Tcp}
import spray.can.Http
import com.mturk.api._
import com.mturk.models.pgdb.DAL

//import scala.slick.driver.MySQLDriver.simple._
import models._

object Boot extends App with MainActors with RootApi {

  //construct database tables; it needs improvement
  DAL.databaseInit()

  implicit lazy val system = ActorSystem("mturk-survey")

  IO(Http) ! Http.Bind(rootService, interface = Config.host, port = Config.portHTTP)
}

object Config {
  import com.typesafe.config.ConfigFactory

  private val config = ConfigFactory.load
  config.checkValid(ConfigFactory.defaultReference)

  val host = config.getString("service.host")
  val portHTTP = config.getInt("service.port")
  val portTcp = config.getInt("service.ports.tcp")
  val portWs = config.getInt("service.ports.ws")

  //database
  val dbURL = config.getString("db.postgresql.url")
  val dbUser = config.getString("db.postgresql.user")
  val dbPassword = config.getString("db.postgresql.password")
  val dbDriver = config.getString("db.postgresql.driver")

  //database one connect string
  val dbConnect = config.getString("db.postgresql.connect")

  //for possible incorrect postgresql setting
  //to prevent weird connection string automatically set up by Dokku's plugin
  if (! dbConnect.contains("postgresql")) dbConnect.replace("postgres", "postgresql")

}