package com.inheater.collector.rest

import com.inheater.collector.mysql.Tables._
import org.jsoup.Jsoup
import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._

import scala.async.Async._
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by cookeem on 16/4/28.
  */
object RestQuery {
  def listSiteQuery(): Future[String] = async {
    val siteQuery = sites.filter(_.isenable === 1)
    val siteResult = await{db.run(siteQuery.result)}
      .map(site => {
      Json.obj(
        "sid" -> site.sid,
        "sitename" -> site.sitename,
        "siteurl" -> site.siteurl
      )
    })
    val json = Json.obj(
      "errmsg" -> "",
      "sitelists" -> siteResult
    )
    Json.stringify(json)
  }.recover{
    case e: Throwable =>
      val json = Json.obj(
        "errmsg" -> s"listSiteQuery error: ${e.getMessage}, ${e.getCause}",
        "sitelists" -> JsArray()
      )
      Json.stringify(json)
  }

  def listContentQuery(sid: Int = 0, pageReq: Int = 1, countReq: Int = 10): Future[String] = async {
    var page = pageReq
    var count = countReq
    if (pageReq < 1) page = 1
    if (countReq < 1) count = 10
    //Left join嵌套
    val contenturlQuery = {
      for {
        (cu, img) <- contenturls joinLeft images on (_.titleimageid === _.imgid)
        site <- sites
        if cu.status === 2 && cu.sid === site.sid && (cu.sid === sid || sid == 0) && ((site.islocal === 1 && cu.imagefinish === 1) || site.islocal === 0)
      } yield (cu.cuid, cu.sid, cu.title, cu.url, cu.author, cu.postdate, cu.contenttext, cu.lastupdate, site.sitename, site.islocal, img.map(_.urlabs).getOrElse(""), img.map(_.path).getOrElse(""), img.map(_.filename).getOrElse(""))
    }
    val contenturlPageQuery = contenturlQuery
      .sortBy{
        case (cuid, sid, title, url, author, postdate, contenttext, lastupdate, sitename, islocal, urlabs, path, filename) => cuid.desc
      }
      .drop((page - 1) * count).take(count)
    val contenturlCountQuery = contenturlQuery.length
    val contenturlResult = await{db.run(contenturlPageQuery.result)}.map{
      case (cuid, sid, title, url, author, postdate, contenttext, lastupdate, sitename, islocal, urlabs, path, filename) => {
        Json.obj(
          "cuid" -> cuid,
          "sid" -> sid,
          "sitename" -> sitename,
          "title" -> title,
          "url" -> url,
          "author" -> author,
          "postdate" -> postdate,
          "content" -> {
            if (contenttext.length > 200)
              contenttext.substring(0, 200)
            else
              contenttext
          },
          "lastupdate" -> lastupdate,
          "titleimage" -> {
            if (islocal == 0) {
              urlabs
            } else {
              s"$path$filename"
            }
          }
        )
      }
    }
    val rscount = await{db.run(contenturlCountQuery.result)}
    val listContentJson = Json.toJson(contenturlResult)
    val json = Json.obj(
      "errmsg" -> "",
      "contentlists" -> listContentJson,
      "rscount" -> rscount
    )
    Json.stringify(json)
  }.recover{
    case e: Throwable =>
      val json = Json.obj(
        "errmsg" -> s"listContentQuery error: ${e.getMessage}, ${e.getCause}",
        "contentlists" -> JsArray(),
        "rscount" -> 0
      )
      Json.stringify(json)
  }

  def contentQuery(cuid: Int): Future[String] = async {
    //Left join嵌套
    val contenturlQuery = {
      for {
        (cu, img) <- contenturls joinLeft images on (_.titleimageid === _.imgid)
        site <- sites
        if cu.status === 2 && cu.sid === site.sid && cu.cuid === cuid && ((site.islocal === 1 && cu.imagefinish === 1) || site.islocal === 0)
      } yield (cu, site.sitename, site.islocal, img.map(_.urlabs).getOrElse(""), img.map(_.path).getOrElse(""), img.map(_.filename).getOrElse(""))
    }
    val (contenturl, sitename, islocal, urlabs, path, filename) = await{db.run(contenturlQuery.result.headOption)}.getOrElse(null, "", 0, "", "", "")
    if (contenturl != null) {
      val tagsQuery = {
        for {
          atags <- articletags
          ctags <- contenttags
          if atags.tid === ctags.tid && ctags.cuid === cuid
        } yield (atags.tid, atags.tagname)
      }
      val tags = await{db.run(tagsQuery.result)}
      val doc = Jsoup.parse(contenturl.contentabsurl, contenturl.url)
      doc.select("img").foreach(img => {
        img.removeAttr("data-w")
        img.removeAttr("data-ratio")
        img.removeAttr("data-src")
        img.removeAttr("width")
        img.removeAttr("height")
        img.removeAttr("style")
        img.removeAttr("class")
      })
      val content = doc.select("html > body > *").toString
      val doc2 = Jsoup.parse(contenturl.contentlocalurl, contenturl.url)
      doc2.select("img").foreach(img => {
        img.removeAttr("data-w")
        img.removeAttr("data-ratio")
        img.removeAttr("data-src")
        img.removeAttr("width")
        img.removeAttr("height")
        img.removeAttr("style")
        img.removeAttr("class")
      })
      val content2 = doc2.select("html > body > *").toString
      val contenturlResult = Json.obj(
        "luid" -> contenturl.luid,
        "sid" -> contenturl.sid,
        "sitename" -> sitename,
        "title" -> contenturl.title,
        "url" -> contenturl.url,
        "author" -> contenturl.author,
        "postdate" -> contenturl.postdate,
        "content" -> {
          if (islocal == 0) {
            content
          } else {
            content2
          }
        },
        "lastupdate" -> contenturl.lastupdate,
        "titleimage" -> {
          if (islocal == 0) {
            urlabs
          } else {
            s"$path$filename"
          }
        },
        "extrainfo" -> contenturl.extrainfo,
        "tags" -> tags.map{case (tagid, tagname) =>
          Json.obj(
            "tagid" -> tagid,
            "tagname" -> tagname
          )
        }
      )
      val listContentJson = Json.toJson(contenturlResult)
      val json = Json.obj(
        "errmsg" -> "",
        "content" -> listContentJson
      )
      Json.stringify(json)
    } else {
      val json = Json.obj(
        "errmsg" -> "content not found",
        "content" -> JsNull
      )
      Json.stringify(json)
    }
  }.recover{
    case e: Throwable =>
      val json = Json.obj(
        "errmsg" -> s"contentQuery error: ${e.getMessage}, ${e.getCause}",
        "content" -> JsNull
      )
      Json.stringify(json)
  }
}
