package com.inheater.collector.http

import java.util.UUID

import com.inheater.collector.common.CommonOps._
import com.inheater.collector.http.ConfigVerify._
import com.inheater.collector.http.HttpFetch._
import org.jsoup.Jsoup
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.util.Random


/**
  * Created by cookeem on 16/4/24.
  */
object SourceParser {
  //解析json格式的列表页
  //input: url:列表页json的url, listJson:json解释格式, isWeixin:是否微信公众号文章, isBrowser:是否模拟浏览器, isScroll:是否模拟浏览器滚屏
  //return: (String:列表页源码, Seq[String]:Seq[内容URL], Int:运行时长毫秒, String:连接错误, String:解释错误)
  def parseListJson(url: String, listJson: String, isWeixin: Int = 0, isBrowser: Int = 0, isScroll: Int = 0): (String, Seq[String], Int, String, String) = {
    val t1 = System.currentTimeMillis
    try {
      var ret = Seq[String]()
      var source = ""
      var connectError = verifyListJson(listJson)
      if (connectError == "") {
        val header = headers(Random.nextInt(headers.length))
        val htmlFetch = sourceFetch(url, 0, 0, 0, 0)
        source = htmlFetch._1
        connectError = htmlFetch._2
        if (connectError == "") {
          val json = Json.parse(source)
          val jsonConf = Json.parse(listJson)
          val contentUrl = jsToString(jsonConf, "contenturl")
          val urlPrefix = jsToString(jsonConf, "urlprefix")
          val extraPrefix = jsToString(jsonConf, "extraprefix")
          val listArr = contentUrl.split(" ")
          var jsonParse = Seq(json)
          for (i <- 1 to (listArr.length / 2)) {
            if (listArr((i-1)*2) == "||") {
              jsonParse = jsonParse.map(js => js \\ listArr((i-1)*2+1)).flatMap(seq => seq.toList)
            } else {
              jsonParse = jsonParse.map(js => (js \ listArr((i-1)*2+1)).get)
            }
          }
          if (urlPrefix != "") {
            jsonParse = jsonParse.filter(js => jsToString(js).startsWith(urlPrefix))
          }
          val urlRoot = url.substring(0,url.lastIndexOf("/"))
          val siteRoot = url.split("/").dropRight(url.split("/").length-3).mkString("/")
          ret = jsonParse.map(js => {
            val str = jsToString(js)
            if (extraPrefix != "" && urlPrefix == "") {
              extraPrefix.replace("<id>", str)
            } else if (!str.startsWith("http://") && !str.startsWith("https://")) {
              if (str.startsWith("/")) {
                s"$siteRoot$str".trim
              } else {
                s"$urlRoot/$str".trim
              }
            } else {
              str
            }
          })
        }
      }
      val t2 = System.currentTimeMillis
      val duration = Math.round(t2 - t1)
      (source, ret, duration, connectError, "")
    } catch {
      case e: Throwable =>
        val t2 = System.currentTimeMillis
        val duration = Math.round(t2 - t1)
        ("", Seq[String](), duration, "", s"parseListJson error: ${e.getMessage}, ${e.getCause}")
    }
  }


  //解析html格式的列表页
  //input: url:列表页html的url, listHtml:html解释格式, isWeixin:是否微信公众号文章, isBrowser:是否模拟浏览器, isScroll:是否模拟浏览器滚屏
  //return: (String:列表页源码, Seq[String]:Seq[内容URL], Int:运行时长毫秒, String:连接错误, String:解释错误)
  def parseListHtml(url: String, listHtml: String, isWeixin: Int = 0, isBrowser: Int = 0, isScroll: Int = 0): (String, Seq[String], Int, String, String) = {
    val t1 = System.currentTimeMillis
    try {
      var ret = Seq[String]()
      var source = ""
      var connectError = verifyListHtml(listHtml)
      if (connectError == "") {
        val htmlFetch = sourceFetch(url, 1, isWeixin, isBrowser, isScroll)
        source = htmlFetch._1
        connectError = htmlFetch._2
        if (connectError == "") {
          val doc = Jsoup.parse(source, url)
          val jsonConf = Json.parse(listHtml)
          val list = jsToString(jsonConf, "list")
          val contentUrl = jsToString(jsonConf, "contenturl")
          val urlPrefix = jsToString(jsonConf, "urlprefix")
          val attr = jsToString(jsonConf, "attr")
          ret = doc.select(list).map(div => {
            //链接抽取
            div.select(contentUrl).map(a => {
              if (attr == "") {
                a.attr("abs:href").trim
              } else {
                a.attr(s"abs:$attr").trim
              }
            }).toSeq
          }).flatMap(x => x).filter(s => s.startsWith(urlPrefix)).toSeq
        }
      }
      val t2 = System.currentTimeMillis
      val duration = Math.round(t2 - t1)
      (source, ret, duration, connectError, "")
    } catch {
      case e: Throwable =>
        val t2 = System.currentTimeMillis
        val duration = Math.round(t2 - t1)
        ("", Seq[String](), duration, "", s"parseListHtml error: ${e.getMessage}, ${e.getCause}")
    }
  }

  //解析json格式的内容页
  //input: url:内容页json的url, contentJson:json解释格式, isWeixin:是否微信公众号文章, isBrowser:是否模拟浏览器, isScroll:是否模拟浏览器滚屏
  //return: (String:内容页源码, Map[String, String]:Map[键值, 内容],包括内容,标题,作者,发布时间,标签,标题图片 Int:运行时长毫秒, String:连接错误, String:解释错误)
  def parseContentJson(url: String, contentJson: String, isWeixin: Int = 0, isBrowser: Int = 0, isScroll: Int = 0): (String, Map[String, String], Int, String, String) = {
//    val url = "http://toutiao.com/api/article/hot_video/?_=1461556369091"
//    val contentJson = """
//      {
//        "content": "| data || abstract",
//        "title": "| data || title",
//        "author": "| data || media_name",
//        "postdate": "| data || datetime",
//        "tag": "| data || keywords",
//        "titleimage": "| data || item_source_url"
//      }
//                      """
    val t1 = System.currentTimeMillis
    try {
      var ret = Map[String, String]()
      var source = ""
      var connectError = verifyContentJson(contentJson)
      if (connectError == "") {
        val header = headers(Random.nextInt(headers.length))
        val htmlFetch = sourceFetch(url, 0, 0, 0, 0)
        source = htmlFetch._1
        connectError = htmlFetch._2
        if (connectError == "") {
          val json = Json.parse(source)
          val jsonConf = Json.parse(contentJson)
          val formatMap =
            Map(
              "content" -> true,
              "title" -> true,
              "author" -> false,
              "postdate" -> false,
              "tag" -> false,
              "titleimage" -> false
            )
          ret = formatMap.map{case (k,v) => {
            val element = jsToString(jsonConf, k)
            if (element != "") {
              try {
                val listArr = element.split(" ")
                var jsonParse = Seq(json)
                for (i <- 1 to (listArr.length / 2)) {
                  if (listArr((i - 1) * 2) == "||") {
                    jsonParse = jsonParse.map(js => js \\ listArr((i - 1) * 2 + 1)).flatMap(seq => seq.toList)
                  } else {
                    jsonParse = jsonParse.map(js => (js \ listArr((i - 1) * 2 + 1)).get)
                  }
                }
                //tag进行特殊处理,以逗号分隔,放在一个字符串中
                if (k == "tag") {
                  (k, jsonParse.map(jsToString).mkString(",").trim)
                } else if (k == "titleimage") {
                  val urlRoot = url.substring(0,url.lastIndexOf("/"))
                  val siteRoot = url.split("/").dropRight(url.split("/").length-3).mkString("/")
                  var titleimage = jsonParse.map(jsToString).get(0)
                  if (!titleimage.startsWith("http://") && !titleimage.startsWith("https://")) {
                    if (titleimage.startsWith("/")) {
                      titleimage = s"$siteRoot$titleimage"
                    } else {
                      titleimage = s"$urlRoot/$titleimage"
                    }
                  }
                  (k, titleimage.trim)
                } else {
                  (k, jsonParse.map(jsToString).get(0).trim)
                }
              } catch {
                case e: Throwable =>
                  (k, "")
              }
            } else {
              (k, "")
            }
          }}
        }
      }
      val t2 = System.currentTimeMillis
      val duration = Math.round(t2 - t1)
      (source, ret, duration, connectError, "")
    } catch {
      case e: Throwable =>
        val t2 = System.currentTimeMillis
        val duration = Math.round(t2 - t1)
        ("", Map[String, String](), duration, "", s"parseContentJson error: ${e.getMessage}, ${e.getCause}")
    }
  }

  //解析html格式的内容页
  //input: url:内容页html的url, contentHtml:html解释格式, isWeixin:是否微信公众号文章, isBrowser:是否模拟浏览器, isScroll:是否模拟浏览器滚屏
  //return: (String:源码, Map[String, String]:Map[键值, 内容],包括内容,标题,作者,发布时间,标签,标题图片 Int:运行时长毫秒, String:连接错误, String:解释错误)
  def parseContentHtml(url: String, contentHtml: String, isWeixin: Int = 0, isBrowser: Int = 0, isScroll: Int = 0): (String, Map[String, String], Int, String, String) = {
//    val url = "http://www.leikeji.com/article/5427"
//    val contentHtml = """
//      {
//        "content": "div[class=article-content]",
//        "title": "div.article-header > h1.article-title",
//        "author": "div[class=article-info clr] > div.user-info > a.username",
//        "postdate": "div[class=article-info clr] > div.user-info > span.time-info",
//        "tag": "ul.tag-list > li.tag-item > a",
//        "titleimage": "div.photo-block > img"
//      }
//                      """
    val t1 = System.currentTimeMillis
    try {
      var ret = Map[String, String]()
      var source = ""
      var connectError = verifyContentHtml(contentHtml)
      if (connectError == "") {
        val htmlFetch = sourceFetch(url, 1, isWeixin, isBrowser, isScroll)
        source = htmlFetch._1
        connectError = htmlFetch._2
        if (connectError == "") {
          val doc = Jsoup.parse(source, url)
          val jsonConf = Json.parse(contentHtml)
          val formatMap =
            Map(
              "content" -> true,
              "title" -> true,
              "author" -> false,
              "postdate" -> false,
              "tag" -> false,
              "titleimage" -> false
            )
          ret = formatMap.map{case (k,v) => {
            val element = jsToString(jsonConf,k)
            if (element != "") {
              try {
                val listArr = element.split(" ")
                //tag进行特殊处理,以逗号分隔,放在一个字符串中
                if (k == "tag") {
                  (k, doc.select(element).map(e => e.text()).mkString(",").trim)
                } else if (k == "titleimage") {
                  (k, doc.select(element).first().attr("abs:src").trim)
                } else if (k == "content") {
                  (k, doc.select(element).first().html().trim)
                } else {
                  (k, doc.select(element).first().text().trim)
                }
              } catch {
                case e: Throwable =>
                  (k, "")
              }
            } else {
              (k, "")
            }
          }}
        }
      }
      val t2 = System.currentTimeMillis
      val duration = Math.round(t2 - t1)
      (source, ret, duration, connectError, "")
    } catch {
      case e: Throwable =>
        val t2 = System.currentTimeMillis
        val duration = Math.round(t2 - t1)
        ("", Map[String, String](), duration, "", s"parseContentHtml error: ${e.getMessage}, ${e.getCause}")
    }
  }

  //解析正文内容,包括:contenttext返回文本正文,contentabsurl修改为绝对路径的内容区源码,contentlocalurl修改为本地图片路径的内容区源码
  //以及图片的:urlabs图片完整URL,urlsrc图片源URL,path本地目录路径,filename文件名
  //input: source:内容区html源码, url:内容页url, sid:站点id
  //return:
  //(Seq[(String, String, String, String)]: Seq(urlabs图片完整URL,urlsrc图片源URL,path本地目录路径,filename文件名),
  // String:contenttext返回文本正文, String:contentabsurl修改为绝对路径的内容区源码, String:contentlocalurl修改为本地图片路径的内容区源码, String:解释错误)
  def parseContentSource(source: String, url: String, sid: Int): (Seq[(String, String, String, String)], String, String, String, String) = {
    var imgSeq = Seq[(String, String, String, String)]()
    val parseError = ""
    try {
      //生成目录
      val datetime = new CurrentDate
      val year = datetime.year
      val month = datetime.month
      val day = datetime.day
      val path = s"upload/$sid/$year$month/$day/"

      //jsoup.parse会自动加上html body
      val docLocalUrl = Jsoup.parse(source, url).select("html > body > *")
      val docAbsUrl = Jsoup.parse(source, url).select("html > body > *")
      imgSeq = docLocalUrl.select("img").map(img => {
        val fileName = UUID.randomUUID().toString()
        val filePath = s"$path$fileName"
        val imgSrc = img.attr("src")
        val imgAbs = img.attr("abs:src")
        img.attr("src", filePath)
        (imgAbs, imgSrc, path, fileName)
      }).toSeq
      docAbsUrl.select("img").foreach(img => {
        img.attr("src", img.attr("abs:src"))
      })
      val contentLocalUrl = docLocalUrl.toString
      val contentAbsUrl = docAbsUrl.toString
      val contentText = docAbsUrl.text()
      (imgSeq, contentText, contentAbsUrl, contentLocalUrl, parseError)
    } catch {
      case e: Throwable =>
        (imgSeq, "", "", "", s"parseContentImage error: ${e.getMessage}, ${e.getCause}")
    }
  }
}
