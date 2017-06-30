package com.inheater.collector.actor

import com.inheater.collector.actor.CollectorOps._

import akka.actor._

/**
  * Created by cookeem on 16/4/27.
  */
case class ImageFetchStart(imgid: Int)

class ImageFetcher(system: ActorSystem) extends Actor with ActorLogging {
  implicit val logs = log

  override def postStop() = {
    log.warning("ImageFetcher begin to stop!")
  }

  def receive = {
    case ImageFetchStart(imgid) =>
//      log.info(s"ImageFetcher receive $imgid")
      fetchImage(imgid)
    case _ =>
      log.error(s"Unacceptable message type")
  }
}

object ImageFetcher {
  def props(system: ActorSystem) = Props(new ImageFetcher(system))
}
