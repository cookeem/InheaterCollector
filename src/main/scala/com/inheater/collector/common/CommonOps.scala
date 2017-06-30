package com.inheater.collector.common

import java.security.MessageDigest

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import play.api.libs.json._

/**
  * Created by cookeem on 16/4/23.
  */
object CommonOps {
  //模拟User Agent的选项
  val headers = Array(
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10; rv:33.0) Gecko/20100101 Firefox/33.0",
    "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; Trident/6.0)",
    "Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US)",
    "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; FunWebProducts)",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A",
    "Mozilla/5.0 (iPad; CPU OS 6_0 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10A5355d Safari/8536.25",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/534.55.3 (KHTML, like Gecko) Version/5.1.3 Safari/534.53.10",
    "Opera/9.80 (Windows NT 6.0) Presto/2.12.388 Version/12.14",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.86 Safari/537.36"
  )

  //http源码请求超时时间,单位秒
  val sourceTimeout = 20
  //http图片请求超时时间,单位秒
  val imageTimeout = 20


  //获取当前时间
  case class CurrentDate() {
    val currentTime = new DateTime()
    val year = currentTime.toString("yyyy")
    val month = currentTime.toString("MM")
    val day = currentTime.toString("dd")
    val hour = currentTime.toString("HH")
    val minute = currentTime.toString("mm")
    val second = currentTime.toString("ss")
  }

  //获取当前时间整数类型
  case class CurrentTimestamp() {
    val datetime = new DateTime()
    val timeMillis = datetime.getMillis
    val timestamp = (timeMillis/ 1000).toInt
    val year = datetime.getYear
    val month = datetime.getMonthOfYear
    val day = datetime.getDayOfMonth
    val week = datetime.getDayOfWeek
  }

  //字符串转时间戳
  def stringToTimestamp(dateStr: String, format: String = "yyyy-MM-dd"): Int = {
    try {
      DateTimeFormat.forPattern(format)
      val dateFormat = DateTimeFormat.forPattern(format)
      val datetime = DateTime.parse(dateStr, dateFormat)
      val time = (datetime.getMillis / 1000).toInt
      time
    } catch {
      case e: Throwable =>
        0
    }
  }

  //时间戳转字符串
  def timestampToString(timestamp: Int, format: String = "yyyy-MM-dd HH:mm:ss"): String = {
    try {
      val dateFormat = DateTimeFormat.forPattern(format)
      val datetime = new DateTime(timestamp * 1000L)
      val dateStr = datetime.toString(dateFormat)
      dateStr
    } catch {
      case e: Throwable =>
        ""
    }
  }

  //md5字符串
  def md5(str: String): String = {
    MessageDigest.getInstance("MD5").digest(str.getBytes).map("%02X".format(_)).mkString
  }

  //JsValue转换成String
  def jsToString(json: JsValue, attr: String): String = {
    if ((json \ attr).getOrElse(JsString("")).isInstanceOf[JsString]) {
      (json \ attr).getOrElse(JsString("")).as[String].trim
    } else {
      (json \ attr).getOrElse(JsString("")).toString.trim
    }
  }

  def jsToString(json: JsValue): String = {
    if (json.isInstanceOf[JsString]) {
      json.as[String].trim
    } else {
      json.toString.trim
    }
  }

  def strToInt(params: Map[String, String], key: String, default: Int): Int = {
    var ret = default
    if (params.contains(key)) {
      try {
        ret = params(key).toInt
      } catch {
        case e: Throwable =>
      }
    }
    ret
  }
}
