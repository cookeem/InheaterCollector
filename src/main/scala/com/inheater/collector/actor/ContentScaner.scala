package com.inheater.collector.actor

import akka.routing.{RoundRobinPool, DefaultResizer}
import com.inheater.collector.actor.CollectorOps._

import akka.actor._
import scala.concurrent.duration._
import scala.async.Async._

/**
  * Created by cookeem on 16/4/27.
  */
case object ContentScanNext

class ContentScaner(system: ActorSystem, interval: Int, routercountMin: Int, routercountMax: Int) extends Actor with ActorLogging {
  import system.dispatcher
  implicit val logs = log
  var lastSid = 0
  val resizer = DefaultResizer(lowerBound = routercountMin, upperBound = routercountMax)
  val router = context.actorOf(RoundRobinPool(routercountMin, Some(resizer)).props(ContentFetcher.props(system)), "router")

  system.scheduler.schedule(0 second, interval milliseconds, self, ContentScanNext)

  override def postStop() = {
    log.warning("ContentScaner begin to stop!")
  }

  def receive = {
    case ContentScanNext =>
      async{
        val sid = await{roundRobinSite(lastSid, 0)}
        if (sid > 0) {
          val (cuid, errmsg) = await{scanContent(sid)}
          if (errmsg == "") {
            router ! ContentFetchStart(cuid)
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

object ContentScaner {
  def props(system: ActorSystem, interval: Int, routercountMin: Int, routercountMax: Int) = Props(new ContentScaner(system, interval, routercountMin, routercountMax))
}

