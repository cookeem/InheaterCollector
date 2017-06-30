package com.inheater.collector

import java.io.File

import com.inheater.collector.actor._
import com.inheater.collector.mysql.TableInit._

import akka.actor._
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by cookeem on 16/4/28.
  */
object CollectorApp extends App {
  //初始化数据库
  Await.result(onTableInit, Duration.Inf)

  val config =  ConfigFactory.parseFile(new File("conf/application.conf"))
  val intervalSite = config.getInt("interval.sitescaner")
  val intervalWxSite = config.getInt("interval.wxsitescaner")
  val intervalList = config.getInt("interval.listscaner")
  val intervalContent = config.getInt("interval.contentscaner")
  val intervalWxContent = config.getInt("interval.wxcontentscaner")
  val intervalImage = config.getInt("interval.imagescaner")
  val intervalIndex = config.getInt("interval.indexscaner")

  val routercountMinContent = config.getInt("routercount.min.contentscaner")
  val routercountMaxContent = config.getInt("routercount.max.contentscaner")
  val routercountMinWxContent = config.getInt("routercount.min.wxcontentscaner")
  val routercountMaxWxContent = config.getInt("routercount.max.wxcontentscaner")
  val routercountMinImage = config.getInt("routercount.min.imagescaner")
  val routercountMaxImage = config.getInt("routercount.max.imagescaner")

  val system = ActorSystem("system", config)

  //启动SiteScaner
  system.actorOf(SiteScaner.props(system, intervalSite), "siteScaner")
  //启动ListScaner
  system.actorOf(ListScaner.props(system, intervalList), "listScaner")
  //启动ContentScaner
  system.actorOf(ContentScaner.props(system, intervalContent, routercountMinContent, routercountMaxContent), "contentScaner")
  //启动ImageScaner
  system.actorOf(ImageScaner.props(system, intervalImage, routercountMinImage, routercountMaxImage), "imageScaner")

  //启动WxSiteScaner
  system.actorOf(WxSiteScaner.props(system, intervalWxSite), "wxSiteScaner")
  //启动WxContentScaner
  system.actorOf(WxContentScaner.props(system, intervalWxContent, routercountMinWxContent, routercountMaxWxContent), "wxContentScaner")

}
