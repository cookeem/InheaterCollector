package com.inheater.collector.http

import java.net.URLEncoder
import java.util.UUID

import com.inheater.collector.common.CommonOps.CurrentDate
import com.inheater.collector.http.HttpFetch._
import org.jsoup.Jsoup

import scala.collection.JavaConversions._

/**
  * Created by cookeem on 16/5/10.
  */
object WeixinParser {
  //爬取搜狗微信文章列表
  //input: keyword:搜索关键字
  //return: (Seq[(String:标题, String:文章url, String:发布日期, String:文章标题图)]: 文章清单, Int:处理时长, String:连接错误信息, String:解释错误信息)
  def parseWxArcitleList(keyword: String): (Seq[(String, String, String, String)], Int, String, String) = {
    val t1 = System.currentTimeMillis
    var articles = Seq[(String, String, String, String)]()
    var connectError = ""
    var parseError = ""
    try {
      val urlList = s"http://weixin.sogou.com/weixin?type=2&query=${URLEncoder.encode(keyword, "UTF-8")}&ie=utf8&_sug_=n&_sug_type_="
      val htmlFetch = sourceFetch(urlList, 1, 0, 0, 0)
      val source = htmlFetch._1
      connectError = htmlFetch._2
      if (connectError == "") {
        val doc = Jsoup.parse(source, urlList)
        val matchArticle = "div[class=weixin-public] div.results div[d]"
        val docMatch = doc.select(matchArticle)
        articles = docMatch.map(div => {
          //抽取文章url
          var urlArticle = ""
          val a = div.select("a[data-z]")
          if (a.nonEmpty) {
            urlArticle = a(0).attr("abs:href")
          }
          //抽取图片
          var titleimageArticle = ""
          val imgs = div.select("a[data-z] > img")
          if (imgs.nonEmpty) {
            titleimageArticle = imgs(0).attr("abs:src")
          }
          //抽取标题
          var titleArticle = ""
          val titles = div.select("div[class=txt-box] > h4 > a")
          if (titles.nonEmpty) {
            titleArticle = titles(0).text()
          }
          //抽取日期
          var postdateArticle = ""
          (titleArticle, urlArticle, postdateArticle, titleimageArticle)
        }).toSeq
        if (articles.isEmpty) {
          parseError = "weixin article list parse error"
        }
      }
      val t2 = System.currentTimeMillis
      val duration = Math.round(t2 - t1)
      (articles, duration, connectError, parseError)
    } catch {
      case e: Throwable =>
        val t2 = System.currentTimeMillis
        val duration = Math.round(t2 - t1)
        (articles, duration, "", s"parseWxArcitleList error: ${e.getMessage}, ${e.getCause}")
    }
  }

  //爬取搜狗微信公众号
  //input: keyword:搜索关键字
  //return: (Seq[(String:标题, String:文章url, String:发布日期, String:文章标题图)]: 文章清单, Int:处理时长, String:连接错误信息, String:解释错误信息)
  def parseWxGzhList(keyword: String): (Seq[(String, String, String, String)], Int, String, String) = {
    val t1 = System.currentTimeMillis
    var articles = Seq[(String, String, String, String)]()
    var connectError = ""
    var parseError = ""
    try {
      val urlList = s"http://weixin.sogou.com/weixin?type=1&query=${URLEncoder.encode(keyword, "UTF-8")}&ie=utf8&_sug_=n&_sug_type_="
      var htmlFetch = sourceFetch(urlList, 1, 0, 0, 0)
      var source = htmlFetch._1
      connectError = htmlFetch._2
      parseError = ""
      if (connectError == "") {
        var doc = Jsoup.parse(source, urlList)
        val matchUrl = "div.results div[href][target]"
        var docMatch = doc.select(matchUrl)
        var urlProfile = ""
        if (docMatch.nonEmpty) {
          urlProfile = doc.select(matchUrl)(0).attr("href")
        } else {
          parseError = "weixin list parse error"
        }
        if (parseError == "") {
          htmlFetch = sourceFetch(urlProfile, 1, 1, 1, 0)
          source = htmlFetch._1
          connectError = htmlFetch._2
          if (connectError == "") {
            doc = Jsoup.parse(source, urlProfile)
            val matchArticleUrl = "div#history div[msgid]"
            docMatch = doc.select(matchArticleUrl)
            articles = docMatch.map(div => {
              //抽取文章url
              //抽取标题
              var urlArticle = ""
              var titleArticle = ""
              val h4Urls = div.select("h4[class=weui_media_title]")
              if (h4Urls.nonEmpty) {
                urlArticle = h4Urls(0).attr("abs:hrefs")
                titleArticle = h4Urls(0).text()
              }
              //抽取图片
              var titleimageArticle = ""
              val divTitleImages = div.select("span[class=weui_media_hd]")
              if (divTitleImages.nonEmpty) {
                titleimageArticle = divTitleImages(0).attr("style").replace("background-image:url(", "").replace(")","")
              }
              //抽取日期
              var postdateArticle = ""
              val divPostdates = div.select("p.weui_media_extra_info")
              if (divPostdates.nonEmpty) {
                postdateArticle = divPostdates(0).text()
              }
              (titleArticle, urlArticle, postdateArticle, titleimageArticle)
            }).toSeq
            if (articles.isEmpty) {
              parseError = "weixin profile parse error"
            }
          }
        }
      }
      val t2 = System.currentTimeMillis
      val duration = Math.round(t2 - t1)
      (articles, duration, connectError, parseError)
    } catch {
      case e: Throwable =>
        val t2 = System.currentTimeMillis
        val duration = Math.round(t2 - t1)
        (articles, duration, "", s"parseWxGzhList error: ${e.getMessage}, ${e.getCause}")
    }
  }

  //爬取微信公众号文章内容
  //input: url:公众号文章url
  //return:
  //String:网页完整源码,
  //Map:文章信息:source网页完整源码, contenttext:返回文本正文, contentabsurl:修改为绝对路径的内容区源码, contentlocalurl:修改为本地图片路径的内容区源码,title,author,postdate,tag,titleimage,readcount,likecount
  //(Seq[(String, String, String, String)]: Seq(urlabs图片完整URL,urlsrc图片源URL,path本地目录路径,filename文件名),
  //String:, Int:连接时长, String:连接错误, String:解释错误)
  def parseWxArticle(url: String, sid: Int): (Map[String, String], Seq[(String, String, String, String)], Int, String, String) = {
    val t1 = System.currentTimeMillis
    var article = Map[String, String]()
    var imgSeq = Seq[(String, String, String, String)]()
    var duration = 0
    var connectError = ""
    var parseError = ""
    try {
      val htmlFetch = sourceFetch(url, 1, 0, 0, 0)
      val source = htmlFetch._1
      connectError = htmlFetch._2
      if (connectError == "") {
        val doc = Jsoup.parse(source, url)
        //抽取标题
        var title = ""
        val titles = doc.select("h2#activity-name")
        if (titles.nonEmpty) {
          title = titles(0).text().trim
        }
        //抽取内容
        var content = ""
        val contents = doc.select("div#js_content")
        if (contents.nonEmpty) {
          content = contents(0).toString.trim
        }
        //抽取发布日期
        var postdate = ""
        val postdates = doc.select("em#post-date")
        if (postdates.nonEmpty) {
          postdate = postdates(0).text().trim
        }
        //抽取作者
        var author = ""
        val authors = doc.select("div.rich_media_meta_list > em[class=rich_media_meta rich_media_meta_text]")
        if (authors.length > 1) {
          author = authors(1).text().trim
        }
        //标题图片
        var titleimage = ""
        val titleimages = doc.select("div#js_content img")
        if (titleimages.length > 2) {
          titleimage = titleimages(1).attr("data-src").trim
        } else if (titleimages.length == 1) {
          titleimage = titleimages(0).attr("data-src").trim
        }
        //阅读数
        var readcount = ""
        var readcounts = doc.select("span#sg_readNum3")
        if (readcounts.nonEmpty) {
          readcount = readcounts(0).text().trim
        }
        //点赞数
        var likecount = ""
        var likecounts = doc.select("span#sg_likeNum3")
        if (likecounts.nonEmpty) {
          likecount = likecounts(0).text().trim
        }

        //内容区图片
        val datetime = new CurrentDate
        val year = datetime.year
        val month = datetime.month
        val day = datetime.day
        val path = s"upload/$sid/$year$month/$day/"
        //jsoup.parse会自动加上html body
        val docLocalUrl = Jsoup.parse(content, url).select("html > body > *")
        val docAbsUrl = Jsoup.parse(content, url).select("html > body > *")
        docAbsUrl.select("img").foreach(img => {
          val imgAbs = img.attr("data-src")
          img.attr("src", imgAbs)
        })
        imgSeq = docLocalUrl.select("img").map(img => {
          val fileName = UUID.randomUUID().toString()
          val filePath = s"$path$fileName"
          val imgAbs, imgSrc = img.attr("data-src")
          img.attr("src", filePath)
          (imgAbs, imgSrc, path, fileName)
        }).toSeq
        val contentlocalurl = docLocalUrl.toString
        val contentabsurl = docAbsUrl.toString
        val contenttext = docAbsUrl.text()

        article = Map(
          "source" -> source,
          "content" -> content,
          "contenttext" -> contenttext,
          "contentabsurl" -> contentabsurl,
          "contentlocalurl" -> contentlocalurl,
          "title" -> title,
          "postdate" -> postdate,
          "author" -> author,
          "titleimage" -> titleimage,
          "readcount" -> readcount,
          "likecount" -> likecount
        )
        if (title == "" || content == "") {
          parseError = "parse weixin article content error"
        }
      }
      val t2 = System.currentTimeMillis
      duration = Math.round(t2 - t1)
    } catch {
      case e: Throwable =>
        val t2 = System.currentTimeMillis
        duration = Math.round(t2 - t1)
        parseError = s"parseWxArticle error: ${e.getMessage}, ${e.getCause}"
    }
    (article, imgSeq, duration, connectError, parseError)
  }

}
