package com.mturk.tasks

import org.json4s.JsonAST.JObject

object Util {

  implicit class jObjectImprovements(val jo: JObject) {
    def isDefined(value: String) = jo.values.get(value).isDefined
    def getValue(value: String): String = jo.values.get(value).get.toString
    def getValueInt(value: String): Option[Int] = {
      import scala.util.control.Exception._
      val s = jo.values.get(value).get.toString
      catching(classOf[NumberFormatException]) opt s.toInt
    }

  }
}
