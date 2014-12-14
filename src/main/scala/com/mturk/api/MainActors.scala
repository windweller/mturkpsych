package com.mturk.api

import akka.actor.Props
import com.mturk.tasks.SECcompany._
import com.mturk.tasks.delayedDiscounting.DelayedDiscountActor
import com.mturk.tasks.mTurkerProgress.MTurkerProgressActor
import com.mturk.tasks.scalaREPL.ProcessActor
import com.mturk.tasks.triadTest.TriadTestActor

//Here is where we build up all actors
trait MainActors {
  this: AbstractSystem =>

  lazy val companyActor = system.actorOf(Props[SECCompanyActor], "SECCompany")
  lazy val mTurkerActor = system.actorOf(Props[MTurkerProgressActor], "mTurker")
  lazy val triadActor = system.actorOf(Props[TriadTestActor], "triad")
  lazy val processActor = system.actorOf(Props(new ProcessActor), "REPLProcess")
  lazy val delayedDiscountActor = system.actorOf(Props[DelayedDiscountActor], "delayedDiscount")

}