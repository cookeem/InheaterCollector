package com.inheater.collector.http

import com.inheater.collector.actor._
import com.inheater.collector.common.CommonOps._

import java.io.{File, FileOutputStream}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by cookeem on 16/4/25.
  */
object HttpFetch {
  val config =  ConfigFactory.parseFile(new File("conf/application.conf"))
  val system = ActorSystem("firefox-system", config)
  import system.dispatcher
  val routercountMinFirefox = config.getInt("routercount.min.firefoxactor")
  val routercountMaxFirefox = config.getInt("routercount.max.firefoxactor")
  val firefoxManager = system.actorOf(FirefoxManager.props(routercountMinFirefox, routercountMaxFirefox), "firefoxManager")
  implicit val timeout = Timeout(300 seconds)

  //获取html源码,支持模拟浏览器以及jsoup方式获取,对于微信公众号进行特殊处理
  //input: url:页面url, isWeixin:是否采集微信公众号, isBrowser:是否模拟浏览器爬取, isScroll:是否模拟浏览器滚屏
  //return: (String:返回的html源码, String:连接错误)
  def sourceFetch(url: String, isHtml: Int, isWeixin: Int, isBrowser: Int, isScroll: Int): (String, String) = {
    var ret = ""
    var connectError = ""
    try {
      if (isBrowser == 0) {
        val header = headers(Random.nextInt(headers.length))
        val ip = s"${110+Random.nextInt(5)}.${1+Random.nextInt(200)}.${1+Random.nextInt(200)}.${1+Random.nextInt(200)}"
        if (isHtml == 1) {
          ret = Jsoup.connect(url).userAgent(header).header("X-Forwarded-For", ip).header("Proxy-Client-IP", ip).header("WL-Proxy-Client-IP", ip).ignoreContentType(true).followRedirects(true).timeout(sourceTimeout*1000).get().toString
        } else {
          ret = Jsoup.connect(url).userAgent(header).header("X-Forwarded-For", ip).header("Proxy-Client-IP", ip).header("WL-Proxy-Client-IP", ip).ignoreContentType(true).followRedirects(true).timeout(sourceTimeout*1000).execute().body()
        }
      } else {
        val FetchResponse(sourceContent, connectErrorMsg) = Await.result(firefoxManager ? FetchRequest(url, isWeixin, isScroll), Duration.Inf)
        connectError = connectErrorMsg
        ret = sourceContent
      }
    } catch {
      case e: Throwable =>
        connectError = s"sourceFetch error: ${e.getMessage}, ${e.getCause}"
    }
    (ret, connectError)
  }

  //jsoup获取图片图片
  //input: imgUrl:图片url绝对路径, path:本地路径, fileName:本地文件名
  //return: (Long:图片大小, Int:运行时长毫秒, String:连接错误)
  def imageFetch(imgUrl: String, path: String, fileName: String): (Long, Int, String) = {
    val t1 = System.currentTimeMillis
    var size = 0L
    val connectError = ""
    try {
      val header = headers(Random.nextInt(headers.length))
      val ip = s"${110+Random.nextInt(5)}.${1+Random.nextInt(200)}.${1+Random.nextInt(200)}.${1+Random.nextInt(200)}"
      val connect = Jsoup.connect(imgUrl).userAgent(header).header("X-Forwarded-For", ip).header("Proxy-Client-IP", ip).header("WL-Proxy-Client-IP", ip).ignoreContentType(true).followRedirects(true).timeout(sourceTimeout*1000).execute()
      val imgBytes = connect.bodyAsBytes()

      //生成目录
      val filePath = new File(path)
      if (!filePath.exists()) {
        filePath.mkdirs()
      }
      val file = new FileOutputStream(s"$path$fileName")
      file.write(imgBytes)
      file.close()
      size = (new File(s"$path$fileName")).length()
      val t2 = System.currentTimeMillis
      val duration = Math.round(t2 - t1)
      (size, duration, connectError)
    } catch {
      case e: Throwable =>
        val t2 = System.currentTimeMillis
        val duration = Math.round(t2 - t1)
        (size, duration, s"imageFetch error: ${e.getMessage}, ${e.getCause}")
    }
  }


}
