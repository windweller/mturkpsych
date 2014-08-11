package com.mturk.tasks.timer

import akka.actor.{Actor, ActorLogging}
import util.ProgressBar

class SECCompanyInitTimers(totalCount: Option[Int]) extends Actor with ActorLogging {
  import SECCompanyInitTimersMsg._

  var currentProgress = 0

  /* Use progress bar to update initialization */

  val bar = new ProgressBar()
  if (totalCount.isDefined) {
    println("Initialization Starts Now!")
    bar.update(currentProgress, totalCount.get)
  }

  def receive = {
    case InitAddOne =>
      currentProgress += 1
      bar.update(currentProgress, totalCount.get)
  }
}

object SECCompanyInitTimersMsg {
  case object InitAddOne
}