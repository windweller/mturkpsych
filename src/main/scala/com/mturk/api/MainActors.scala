package com.mturk.api

import akka.actor.Props
import com.mturk.tasks.SECcompany._
import com.mturk.tasks.mTurkerProgress.MTurkerProgressActor

/**
 * Created by Aimingnie on 5/14/14.
 */
//Here is where we build up all actors
trait MainActors {
  this: AbstractSystem =>

  lazy val companyActor = system.actorOf(Props[SECCompanyActor], "SECCompany")
  lazy val mTurkerActor = system.actorOf(Props[MTurkerProgressActor], "mTurker")

}
