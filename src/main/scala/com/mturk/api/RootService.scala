package com.mturk.api

import spray.routing.{HttpService, Route}
import akka.actor.{ActorLogging, Actor}

/**
 * RootService handles the static routes to common stuff,
 * as well as the routing to all other services;
 */
class RootService(route: Route) extends Actor with HttpService with ActorLogging{
  implicit def actorRefFactory = context
  def receive = runRoute(route)
}
