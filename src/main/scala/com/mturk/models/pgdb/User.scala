package com.mturk.models.pgdb

import java.sql.Timestamp
import java.util.UUID

import org.joda.time.DateTime

import scala.slick.driver.PostgresDriver.simple._

object User {
  case class User(id: Option[Int], name: Option[String], email: String, password: String,
                  userAssignedToken: Option[String], createdTime: Timestamp, auth_level: Byte)

  class UserTable(tag: Tag) extends Table[User](tag, "User") {
    def id = column[Option[Int]]("USER_ID", O.PrimaryKey, O.AutoInc)
    def name = column[Option[String]]("USER_NAME", O.Nullable)
    def email = column[String]("USER_EMAIL", O.Nullable)
    def password = column[String]("USER_PASSWORD", O.Nullable)
    def userAssignedToken = column[Option[String]]("USER_ASSIGNED_TOKEN")
    def createdTime = column[Timestamp]("USER_CREATEDTIME")
    def auth_level = column[Byte]("AUTH_LEVEL")            //Highest level: 16

    def * = (id, name, email, password, userAssignedToken, createdTime, auth_level) <> (User.tupled, User.unapply _)
  }

  val users = TableQuery[UserTable]

  def insert(user: User)(implicit s: Session): User = {
    val uuid = UUID.randomUUID().toString
    val userId = (users returning users.map(_.id)) += user.copy(userAssignedToken = Some(uuid))
    user.copy(id=userId, userAssignedToken = Some(uuid))
  }

  def getFromEmail(email: String)(implicit s: Session) = {
    val userQuery = for (u <- users if u.email === email) yield u
    userQuery.take(1).list
  }

  def getByToken(token: String)(implicit s: Session) = {
    val userQuery = for (u <- users if u.userAssignedToken === token) yield u
    userQuery.take(1).list
  }

}
