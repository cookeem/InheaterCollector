package com.inheater.collector.actor

import com.inheater.collector.actor.CollectorOps._

import akka.actor._
import scala.concurrent.duration._

/**
  * Created by cookeem on 16/4/27.
  */
case object WxSiteScanStart

class WxSiteScaner(system: ActorSystem, interval: Int) extends Actor with ActorLogging {
  import system.dispatcher
  implicit val logs = log
  system.scheduler.schedule(0 second, interval milliseconds, self, WxSiteScanStart)

  override def postStop() = {
    log.warning("WxSiteScaner begin to stop!")
  }

  def receive = {
    case WxSiteScanStart =>
      scanWxSite
    case _ =>
      log.error(s"Unacceptable message type")
  }
}

object WxSiteScaner {
  def props(system: ActorSystem, interval: Int) = Props(new WxSiteScaner(system, interval))
}

