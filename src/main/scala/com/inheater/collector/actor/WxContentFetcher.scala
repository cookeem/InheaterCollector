package com.inheater.collector.actor

import com.inheater.collector.actor.CollectorOps._

import akka.actor._

/**
  * Created by cookeem on 16/5/11.
  */
case class WxContentFetchStart(cuid: Int)

class WxContentFetcher(system: ActorSystem) extends Actor with ActorLogging {
  implicit val logs = log

  override def postStop() = {
    log.warning("WxContentFetcher begin to stop!")
  }

  def receive = {
    case WxContentFetchStart(cuid) =>
      //      log.info(s"WxContentFetcher receive $cuid")
      fetchWxContent(cuid)
    case _ =>
      log.error(s"Unacceptable message type")
  }
}

object WxContentFetcher {
  def props(system: ActorSystem) = Props(new WxContentFetcher(system))
}
