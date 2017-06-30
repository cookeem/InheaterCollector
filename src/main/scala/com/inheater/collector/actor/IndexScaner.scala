package com.inheater.collector.actor

import com.inheater.collector.actor.CollectorOps._

import akka.actor._
import scala.concurrent.duration._

/**
  * Created by cookeem on 16/4/27.
  */
case object IndexScanStart

class IndexScaner(system: ActorSystem, interval: Int) extends Actor with ActorLogging {
  import system.dispatcher
  implicit val logs = log
  system.scheduler.schedule(0 second, interval milliseconds, self, IndexScanStart)

  override def postStop() = {
    log.warning("IndexScaner begin to stop!")
  }

  def receive = {
    case IndexScanStart =>
      scanIndexContent(1)
    case _ =>
      log.error(s"Unacceptable message type")
  }
}

object IndexScaner {
  def props(system: ActorSystem, interval: Int) = Props(new IndexScaner(system, interval))
}
