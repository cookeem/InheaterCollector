package com.inheater.collector.actor

import com.inheater.collector.actor.CollectorOps._

import akka.actor._

/**
  * Created by cookeem on 16/4/27.
  */
case class ContentFetchStart(cuid: Int)

class ContentFetcher(system: ActorSystem) extends Actor with ActorLogging {
  implicit val logs = log

  override def postStop() = {
    log.warning("ContentFetcher begin to stop!")
  }

  def receive = {
    case ContentFetchStart(cuid) =>
//      log.info(s"ContentFetcher receive $cuid")
      fetchContent(cuid)
    case _ =>
      log.error(s"Unacceptable message type")
  }
}

object ContentFetcher {
  def props(system: ActorSystem) = Props(new ContentFetcher(system))
}
