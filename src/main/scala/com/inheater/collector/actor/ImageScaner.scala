package com.inheater.collector.actor

import akka.routing.{RoundRobinPool, DefaultResizer}
import com.inheater.collector.actor.CollectorOps._

import akka.actor._
import scala.concurrent.duration._
import scala.async.Async._

/**
  * Created by cookeem on 16/4/27.
  */
case object ImageScanNext

class ImageScaner(system: ActorSystem, interval: Int, routercountMin: Int, routercountMax: Int) extends Actor with ActorLogging {
  import system.dispatcher
  implicit val logs = log
  var lastSid = 0
  val resizer = DefaultResizer(lowerBound = routercountMin, upperBound = routercountMax)
  val router = context.actorOf(RoundRobinPool(routercountMin, Some(resizer)).props(ImageFetcher.props(system)), "router")

  system.scheduler.schedule(0 second, interval milliseconds, self, ImageScanNext)

  override def postStop() = {
    log.warning("ImageScaner begin to stop!")
  }

  def receive = {
    case ImageScanNext =>
      async{
        val sid = await{roundRobinSite(lastSid)}
        if (sid > 0) {
          val (imgid, errmsg) = await{scanImage(sid)}
          if (errmsg == "" && imgid > 0) {
            router ! ImageFetchStart(imgid)
          } else {
//            log.warning("No image imgid to process")
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

object ImageScaner {
  def props(system: ActorSystem, interval: Int, routercountMin: Int, routercountMax: Int) = Props(new ImageScaner(system, interval, routercountMin, routercountMax))
}
