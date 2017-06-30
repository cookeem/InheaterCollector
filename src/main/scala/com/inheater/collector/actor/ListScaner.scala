package com.inheater.collector.actor

import com.inheater.collector.actor.CollectorOps._

import akka.actor._
import scala.concurrent.duration._
import scala.async.Async._

/**
  * Created by cookeem on 16/4/27.
  */
case object ListScanStart

class ListScaner(system: ActorSystem, interval: Int) extends Actor with ActorLogging {
  import system.dispatcher
  implicit val logs = log
  var lastSid = 0
  system.scheduler.schedule(0 second, interval milliseconds, self, ListScanStart)

  override def postStop() = {
    log.warning("ListScaner begin to stop!")
  }

  def receive = {
    case ListScanStart =>
      async{
        val sid = await{roundRobinSite(lastSid, 0)}
        if (sid > 0) {
          val (luid, errmsg) = await{scanList(sid)}
          if (errmsg == "") {
            await{fetchList(luid)}
          }
          lastSid = sid
        } else {
//          log.warning("No site sid to process")
        }
      }
    case _ =>
      log.error(s"Unacceptable message type")
  }
}

object ListScaner {
  def props(system: ActorSystem, interval: Int) = Props(new ListScaner(system, interval))
}

