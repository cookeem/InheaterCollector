package com.inheater.collector.actor

import akka.routing.{RoundRobinPool, DefaultResizer}
import com.inheater.collector.actor.CollectorOps._

import akka.actor._
import scala.concurrent.duration._
import scala.async.Async._

/**
  * Created by cookeem on 16/5/11.
  */
case object WxContentScanNext

class WxContentScaner(system: ActorSystem, interval: Int, routercountMin: Int, routercountMax: Int) extends Actor with ActorLogging {
  import system.dispatcher
  implicit val logs = log
  var lastSid = 0
  val resizer = DefaultResizer(lowerBound = routercountMin, upperBound = routercountMax)
  val router = context.actorOf(RoundRobinPool(routercountMin, Some(resizer)).props(WxContentFetcher.props(system)), "router")

  system.scheduler.schedule(0 second, interval milliseconds, self, WxContentScanNext)

  override def postStop() = {
    log.warning("WxContentScaner begin to stop!")
  }

  def receive = {
    case WxContentScanNext =>
      async{
        val sid = await{roundRobinSite(lastSid, 1)}
        if (sid > 0) {
          val (cuid, errmsg) = await{scanWxContent(sid)}
          if (errmsg == "") {
            router ! WxContentFetchStart(cuid)
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

object WxContentScaner {
  def props(system: ActorSystem, interval: Int, routercountMin: Int, routercountMax: Int) = Props(new WxContentScaner(system, interval, routercountMin, routercountMax))
}

