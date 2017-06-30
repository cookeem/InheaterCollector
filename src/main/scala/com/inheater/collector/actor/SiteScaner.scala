package com.inheater.collector.actor

import com.inheater.collector.actor.CollectorOps._

import akka.actor._
import scala.concurrent.duration._

/**
  * Created by cookeem on 16/4/27.
  */
case object SiteScanStart

class SiteScaner(system: ActorSystem, interval: Int) extends Actor with ActorLogging {
  import system.dispatcher
  implicit val logs = log
  system.scheduler.schedule(0 second, interval milliseconds, self, SiteScanStart)

  override def postStop() = {
    log.warning("SiteScaner begin to stop!")
  }

  def receive = {
    case SiteScanStart =>
      scanSite
    case _ =>
      log.error(s"Unacceptable message type")
  }
}

object SiteScaner {
  def props(system: ActorSystem, interval: Int) = Props(new SiteScaner(system, interval))
}

