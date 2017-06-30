package com.inheater.collector.actor

import java.util.UUID

import com.inheater.collector.common.CommonOps._
import com.inheater.collector.http.ConfigVerify._
import com.inheater.collector.http.HttpFetch._
import com.inheater.collector.http.SourceParser._
import com.inheater.collector.http.WeixinParser._
import com.inheater.collector.mysql.Tables._

import akka.event.LoggingAdapter
import play.api.libs.json.{JsNumber, JsString, Json}
import slick.jdbc.MySQLProfile.api._

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by cookeem on 16/4/26.
  */
object CollectorOps {
//  //自定义executionContext
//  implicit val ec = new ExecutionContext {
//    val threadPool = Executors.newFixedThreadPool(1024)
//    def execute(runnable: Runnable) {
//      threadPool.submit(runnable)
//    }
//    def reportFailure(e: Throwable) {}
//  }

  //初始化设置UTF8MB4编码
  val sqlUtf8mb4 = sql"SET NAMES 'utf8mb4'"
  Await.ready(db.run(sqlUtf8mb4.asUpdate), Duration.Inf)

  //全量检查site数据表的非微信公众号部分站点是否存在格式异常:urlformat, listjson, listhtml, contentjson, contenthtml
  //并生成listurls表数据
  def scanSite(implicit log: LoggingAdapter): Future[Unit] = async{
    val errmsg = ""
    //注意,这里不爬取微信公众号,微信公众号不生成listurls记录
    val siteQuery = sites.filter(_.isweixin === 0).map(site => (site.sid, site.isenable, site.listcreated, site.urlformat, site.listjson, site.listhtml, site.contentjson, site.contenthtml))
    await{db.run(siteQuery.result)}.foreach{
      case (sid, isenable, listcreated, urlformat, listjson, listhtml, contentjson, contenthtml) => {
        var errmsg = ""
        if (urlformat == "") {
          errmsg = "urlformat为空"
        } else {
          //校验urlformat并生成listurl
          errmsg = verifyUrlFormat(urlformat)._2
          if (errmsg == "") {
            //检查listjson和listhtml必填,并且两者选一
            var unique = 0
            if (listjson.trim != "") {
              unique += 1
            }
            if (listhtml.trim != "") {
              unique += 1
            }
            if (unique != 1) {
              errmsg = "listjson和listhtml必填,并且两者选一"
            }
            if (errmsg == "") {
              //检查listjson和listhtml格式是否正确
              if (listjson.trim != "") {
                errmsg = verifyListJson(listjson)
              }
              if (errmsg == "") {
                if (listhtml.trim != "") {
                  errmsg = verifyListHtml(listhtml)
                }
                if (errmsg == "") {
                  //检查contentjson和contenthtml必填,并且两者选一
                  var unique = 0
                  if (contentjson.trim != "") {
                    unique += 1
                  }
                  if (contenthtml.trim != "") {
                    unique += 1
                  }
                  if (unique != 1) {
                    errmsg = "contentjson和contenthtml必填,并且两者选一"
                  }
                  if (errmsg == "") {
                    //检查contentjson和contenthtml格式是否正确
                    if (contentjson.trim != "") {
                      errmsg = verifyContentJson(contentjson)
                    }
                    if (errmsg == "") {
                      if (contenthtml.trim != "") {
                        errmsg = verifyContentHtml(contenthtml)
                      }
                    }
                  }
                }
              }
            }
          }
        }
        if (errmsg != "") {
          //假如校验有问题,设置sites数据表的isenable和parseerror信息
          val siteUpdateQuery = sites.filter(_.sid === sid)
            .map(site => (site.isenable, site.listcreated, site.parseerror))
            .update((0, 0, errmsg))
          Await.ready(db.run(siteUpdateQuery), Duration.Inf)
          //假如校验有问题,设置listurls数据表的isenable信息
          val listurlUpdateQuery = listurls.filter(_.sid === sid)
            .map(lu => (lu.isenable))
            .update((0))
          Await.ready(db.run(listurlUpdateQuery), Duration.Inf)
        } else {
          //假如校验正确,比较原listurl插入新listurl,更新旧listurl
          if (listcreated == 0) {
            upsertListUrl(sid)
          }
          //假如校验正确,设置listcreated和parseerror信息
          val siteUpdateQuery = sites.filter(_.sid === sid)
            .map(site => (site.listcreated, site.parseerror))
            .update((1, ""))
          Await.ready(db.run(siteUpdateQuery), Duration.Inf)
        }
    }}
    if (errmsg == "") {
      log.info(s"scanSite success")
    } else {
      log.error(s"scanSite error: $errmsg")
    }
  }.recover{
    case e: Throwable =>
      log.error(s"scanSite error: ${e.getMessage}, ${e.getCause}")
  }

  //比较原listurl插入新listurl,更新旧listurl(千万不要在future中嵌套future,否则会丢线程)
  //input: sid:站点ID
  //return: String: 错误提示
  def upsertListUrl(sid: Int)(implicit log: LoggingAdapter): Unit = {
    var errmsg = ""
    var insertCount = 0
    try {
      //检查对应sid的sites数据是否存在
      val siteQuery = sites.filter(site => site.sid === sid && site.isweixin === 0).map(site => (site.sid, site.isenable, site.listcreated, site.entryurl, site.urlformat))
      val (sidCheck: Int, isenable, listcreated, entryurl, urlformat) = Await.result(db.run(siteQuery.result.headOption), Duration.Inf).getOrElse((0, 0, 0, "", ""))
      //只更新listcreated字段=0的数据
      if (sidCheck > 0 && listcreated == 0) {
        val vuf = verifyUrlFormat(urlformat)
        //新的listurl数据
        var listurlNew = vuf._1
        if (entryurl.trim != "") {
          listurlNew = entryurl +: listurlNew
        }
        errmsg = vuf._2
        if (errmsg == "") {
          val firstUrl = listurlNew.head
          val listurlQuery = listurls.filter(_.sid === sid).map(lu => (lu.luid, lu.listurl))
          //旧的listurl数据
          val listurlOld: Seq[(Int, String)] = Await.result(db.run(listurlQuery.result), Duration.Inf)
          //需要新插入的listurl
          val listurlInsert: Seq[String] = listurlNew.diff(listurlOld.map(_._2))
          //需要停止的listurl
          val listurlStop = listurlOld.filter(t => !listurlNew.contains(t._2))
          //新旧都存在的listurl
          val listurlExist= listurlOld.filter(t => listurlNew.contains(t._2))
          //插入listurlInsert
          listurlInsert.foreach(lu => {
            val dateline = (new CurrentTimestamp).timestamp
            val insListUrl = (listurls returning listurls.map(_.luid)) +=
              ListUrl(0, sid, lu, "", dateline, 0, 0, 0, 0, isenable, 0, "", "", 0)
            Await.ready(db.run(insListUrl), Duration.Inf)
          })
          insertCount = listurlInsert.length
          //停止listurlStop,设置isenable=0
          val listurlStopUpdateQuery = listurls.filter(lu => lu.sid === sid && lu.luid.inSet(listurlStop.map(_._1)))
            .map(_.isenable).update(0)
          Await.ready(db.run(listurlStopUpdateQuery), Duration.Inf)
          //更新isfirst字段
          Await.ready(db.run(listurls.filter(_.sid === sid).map(_.isfirst).update(0)), Duration.Inf)
          Await.ready(db.run(listurls.filter(lu => lu.sid === sid && lu.listurl === firstUrl).map(_.isfirst).update(1)), Duration.Inf)
        }
      }
    } catch {
      case e: Throwable =>
        errmsg = s"upsertListUrl error: sid = $sid, ${e.getMessage}, ${e.getCause}"
    }
    if (errmsg != "") {
      log.error(s"upsertListUrl error: sid = $sid, $errmsg")
    } else {
      log.info(s"upsertListUrl success: sid = $sid, insertCount = $insertCount")
    }
  }

  //全量检查site数据表的微信公众号部分站点是否存在格式异常:wxconfig
  //并直接生成contenturl列表
  def scanWxSite(implicit log: LoggingAdapter): Future[Unit] = async{
    val errmsg = ""
    //注意,这里只爬取微信公众号,微信公众号不生成listurls记录
    val siteQuery = sites.filter(_.isweixin === 1).map(site => (site.sid, site.isenable, site.wxconfig))
    await{db.run(siteQuery.result)}.foreach{
      case (sid, isenable, wxconfig) => {
        var errmsg = ""
        if (wxconfig == "") {
          errmsg = "wxconfig为空"
        } else {
          //校验urlformat并生成listurl
          errmsg = verifyWxConfig(wxconfig)
        }
        if (errmsg == "") {
          //假如校验正确,直接生成contenturl列表
          if (isenable == 1) {
            upsertWxContentUrl(sid)
          }
          //假如校验正确,设置listcreated和parseerror信息
          val siteUpdateQuery = sites.filter(_.sid === sid)
            .map(site => (site.parseerror))
            .update((""))
          Await.ready(db.run(siteUpdateQuery), Duration.Inf)
        } else {
          val siteUpdateQuery = sites.filter(_.sid === sid)
            .map(site => (site.isenable, site.listcreated, site.parseerror))
            .update((0, 0, errmsg))
          Await.ready(db.run(siteUpdateQuery), Duration.Inf)
        }
      }}
    if (errmsg == "") {
      log.info(s"scanWxSite success")
    } else {
      log.error(s"scanWxSite error: $errmsg")
    }
  }.recover{
    case e: Throwable =>
      log.error(s"scanSite error: ${e.getMessage}, ${e.getCause}")
  }

  //生成并插入微信公众号contenturl
  //input: sid:站点ID
  //return: String: 错误提示
  def upsertWxContentUrl(sid: Int)(implicit log: LoggingAdapter): Unit = {
    var errmsg = ""
    var dateline = (new CurrentTimestamp).timestamp
    var insertCount = 0
    try {
      //检查对应sid的sites数据是否存在
      val siteQuery = sites.filter(site => site.sid === sid && site.isweixin === 1 && site.isenable === 1).map(site => (site.wxconfig, site.schedule, site.lastfetch))
      val (wxconfig, schedule, lastfetch) = Await.result(db.run(siteQuery.result.headOption), Duration.Inf).getOrElse(("", 0, 0))
      //只更新listcreated字段=0的数据
      if (wxconfig != "") {
        errmsg = verifyWxConfig(wxconfig)
        if (errmsg == "" && dateline - schedule > lastfetch) {
          val json = Json.parse(wxconfig)
          val keyword = (json \ "keyword").getOrElse(JsString("")).as[String]
          val contenttype = (json \ "contenttype").getOrElse(JsNumber(0)).as[Int]
          var pwx = (Seq[(String, String, String, String)](), 0, "", "")
          if (contenttype == 0) {
            pwx = parseWxArcitleList(keyword)
          } else if (contenttype == 1) {
            pwx = parseWxGzhList(keyword)
          }
          val (articles, duration, connectError, parseError) = pwx
          //更新sites的lastfetch以及listcreated
          Await.ready(db.run(sites.filter(_.sid === sid).map(site => (site.lastfetch, site.listcreated)).update((dateline, 1))), Duration.Inf)
          //插入内容页url
          dateline = (new CurrentTimestamp).timestamp
          articles.reverse.foreach{case (title, url, postdate, titleimage) => {
            //根据标题识别公众号文章是否重复
            val titleFound = Await.result(db.run(contenturls.filter(cnturl => cnturl.sid === sid && cnturl.title === title).length.result), Duration.Inf)
            if (titleFound == 0) {
              val insContentUrl = (contenturls returning contenturls.map(_.cuid)) +=
                ContentUrl(0, 0, sid, url, "", postdate, dateline, 0, title, "", "", "", "", "", "", 0, 0, 0, 0, "", "", 0)
              val cuid = Await.result(db.run(insContentUrl), Duration.Inf)
//              var titleimageid = 0
//              if (titleimage.trim != "") {
//                val datetime = new CurrentDate
//                val year = datetime.year
//                val month = datetime.month
//                val day = datetime.day
//                val path = s"upload/$sid/$year$month/$day/"
//                val fileName = UUID.randomUUID().toString()
//                val insImage = (images returning images.map(_.imgid)) +=
//                  Image(0, cuid, sid, titleimage, titleimage, dateline, 0, path, fileName, 0, 0, "", 0)
//                titleimageid = Await.result(db.run(insImage), Duration.Inf)
//                Await.ready(db.run(contenturls.filter(_.cuid === cuid).map(_.titleimageid).update(titleimageid)), Duration.Inf)
//              }
              insertCount += 1
            }
          }}
        }
      } else {
        errmsg = s"upsertWxContentUrl error: sid = $sid, site not found"
      }
    } catch {
      case e: Throwable =>
        errmsg = s"upsertWxContentUrl error: sid = $sid, ${e.getMessage}, ${e.getCause}"
    }
    if (errmsg != "") {
      log.error(s"upsertWxContentUrl error: sid = $sid, $errmsg")
    } else {
      if (insertCount > 0) {
        log.info(s"upsertWxContentUrl success: sid = $sid, insertCount = $insertCount")
      }
    }
  }

  //定期扫描listurls表,并获取一条符合条件的luid
  //return: Future[Int, String]: Int:符合条件的luid, String:错误提示
  def scanList(sid: Int)(implicit log: LoggingAdapter): Future[(Int, String)] = async{
    val dateline = (new CurrentTimestamp).timestamp
    val listurlQuery = {
      for {
        lu <- listurls
        site <- sites
        if site.sid === sid && lu.sid === site.sid &&
          //查询listurs.isenable=1并且(listurs待处理 或者 listurs已处理的isfirst)
          ((lu.isenable === 1 && lu.status === 0 && site.lastfetch+site.interval < dateline) || (lu.isenable === 1 && lu.isfirst === 1 && (lu.status === 2 || lu.status === 3) && site.lastfetch+site.schedule < dateline))
      } yield (lu.luid, lu.listurl)
    }.sortBy{
      case (luid, url) => luid.desc
    }.take(1)
    val (luid, url) = await{db.run(listurlQuery.result.headOption)}.getOrElse(0, "")
    if (luid > 0) {
      log.info(s"scanList success: luid = $luid")
    }
    (luid, "")
  }.recover{
    case e: Throwable =>
      val errmsg = s"scanList error:, ${e.getMessage}, ${e.getCause}"
      log.error(errmsg)
      (0, errmsg)
  }

  //爬取指定的列表页,更新listurls表,插入contenturls表
  def fetchList(luid: Int)(implicit log: LoggingAdapter): Future[Unit] = async{
    var errmsg = ""
    var dateline = (new CurrentTimestamp).timestamp
    val listurlQuery = {
      for {
        lu <- listurls
        site <- sites
        if lu.sid === site.sid && lu.luid === luid && lu.isenable === 1
      } yield (lu, site)
    }
    val (listurl , site) = await{db.run(listurlQuery.result.headOption)}.getOrElse(null, null)
    var source = ""
    var sid = 0
    var contentUrls = Seq[String]()
    var duration = 0
    var connectError = ""
    var parseError = ""
    var connectCount = 0
    var parseCount = 0
    if (listurl != null && site != null) {
      sid = listurl.sid
      //更新sites站点表的lastfetch
      await{db.run(sites.filter(_.sid === sid).map(_.lastfetch).update(dateline))}
      connectCount = listurl.connectcount
      parseCount = listurl.parsecount
      //更新列表页url为待更新状态
      await{db.run(listurls.filter(_.luid === luid).map(lu => (lu.status, lu.lastupdate)).update((1, dateline)))}
      if (site.listhtml.trim != "") {
        val parseResult = parseListHtml(listurl.listurl, site.listhtml, 0, site.isbrowser, site.isscroll)
        source = parseResult._1
        contentUrls = parseResult._2
        duration = parseResult._3
        connectError = parseResult._4
        parseError = parseResult._5
      } else if (site.listjson.trim != "") {
        val parseResult = parseListJson(listurl.listurl, site.listjson, 0, site.isbrowser, site.isscroll)
        source = parseResult._1
        contentUrls = parseResult._2
        duration = parseResult._3
        connectError = parseResult._4
        parseError = parseResult._5
      } else {
        errmsg = s"fetchList error: luid = $luid, site listhtml and listjson empty!"
      }
    } else {
      if (luid > 0) {
        errmsg = s"fetchList error: luid = $luid, listurl or site not found!"
      }
    }
    var status = 1
    //更新列表页url
    if (errmsg != "") {
      status = 4
    } else if (connectError != "") {
      status = 3
    } else if (parseError != "") {
      status = 4
    } else {
      status = 2
    }
    dateline = (new CurrentTimestamp).timestamp
    Await.ready(db.run(
      listurls.filter(_.luid === luid)
      .map(lu => (lu.source, lu.status, lu.lastupdate, lu.duration, lu.connectcount, lu.parsecount, lu.connecterror, lu.parseerror))
      .update((source, status, dateline, duration, connectCount+1, parseCount+1, connectError, parseError))
    ), Duration.Inf)

    //插入内容页url
    dateline = (new CurrentTimestamp).timestamp
    contentUrls.reverse.foreach(cu => {
      val urlFound = Await.result(db.run(contenturls.filter(_.url === cu).length.result), Duration.Inf)
      if (urlFound == 0) {
        val insContentUrl = (contenturls returning contenturls.map(_.cuid)) +=
          ContentUrl(0, luid, sid, cu, "", "", dateline, 0, "", "", "", "", "", "", "", 0, 0, 0, 0, "", "", 0)
        Await.ready(db.run(insContentUrl), Duration.Inf)
//        log.info(s"fetchList insert contenturl: luid = $luid, url = $cu")
      }
    })
    if (errmsg != "") {
      log.error(s"fetchList error: $errmsg")
    } else {
      if (luid > 0) {
        log.info(s"fetchList success: luid = $luid")
      }
    }
  }.recover{
    case e: Throwable =>
      val errmsg = s"fetchList error: luid = $luid, ${e.getMessage}, ${e.getCause}"
      log.error(errmsg)
  }

  //定期扫描contenturls表(非微信公众号文章),并获取一条符合条件的cuid
  //return: Future[Int, String]: Int:符合条件的cuid, String:错误提示
  def scanContent(sid: Int)(implicit log: LoggingAdapter): Future[(Int, String)] = async{
    val errmsg = ""
    val dateline = (new CurrentTimestamp).timestamp
    val contenturlQuery = {
      for {
        cu <- contenturls
        site <- sites
        //注意,这里不扫描微信公众号的文章
        if cu.sid === site.sid && cu.sid === sid && site.isweixin === 0 &&
          //查询contenturs.isenable=1并且(listurs待处理 或者 listurs已处理的isfirst)
          (cu.status === 0 || cu.status === 3) && site.lastfetch+site.interval < dateline
      } yield (cu.cuid, cu.url, site.lastfetch)
    }.sortBy{
      case (cuid, url, lastfetch) => lastfetch.desc
    }.take(1)
    val (cuid, url, lastfetch) = await{db.run(contenturlQuery.result.headOption)}.getOrElse(0, "", 0)
    if (cuid > 0) {
      log.info(s"scanContent success: sid = $sid, cuid = $cuid, url = $url")
    }
    (cuid, errmsg)
  }.recover{
    case e: Throwable =>
      val errmsg = s"scanContent error: sid = $sid, ${e.getMessage}, ${e.getCause}"
      log.error(errmsg)
      (0, errmsg)
  }

  //爬取指定的内容页(非微信公众号文章),更新contenturls表
  def fetchContent(cuid: Int)(implicit log: LoggingAdapter): Future[Unit] = async{
    var errmsg = ""
    var dateline = (new CurrentTimestamp).timestamp
    val contenturlQuery = {
      for {
        cu <- contenturls
        site <- sites
        if cu.sid === site.sid && cu.cuid === cuid
      } yield (cu, site)
    }
    val (contenturl, site) = await{db.run(contenturlQuery.result.headOption)}.getOrElse(null, null)
    var url = ""
    var source = ""
    var sid = 0
    var postdate = ""
    var title = ""
    var author = ""
    var contentsource = ""
    var tag = ""
    var titleimage = ""
    var duration = 0
    var connectError = ""
    var parseError = ""
    if (contenturl != null && site != null) {
      url = contenturl.url
      sid = contenturl.sid
      //更新sites站点表的lastfetch
      Await.ready(db.run(sites.filter(_.sid === sid).map(_.lastfetch).update(dateline)), Duration.Inf)
      //更新列表页url为待更新状态
      Await.ready(db.run(contenturls.filter(_.cuid === cuid).map(cu => (cu.status, cu.lastupdate)).update((1, dateline))), Duration.Inf)
      if (site.contenthtml.trim != "") {
        val parseResult = parseContentHtml(contenturl.url, site.contenthtml, 0, site.isbrowser, site.isscroll)
        source = parseResult._1
        if (parseResult._2.contains("postdate")) {
          postdate = parseResult._2("postdate")
        }
        if (parseResult._2.contains("title")) {
          title = parseResult._2("title")
        }
        if (parseResult._2.contains("author")) {
          author = parseResult._2("author")
        }
        if (parseResult._2.contains("content")) {
          contentsource = parseResult._2("content")
        }
        if (parseResult._2.contains("tag")) {
          tag = parseResult._2("tag")
        }
        if (parseResult._2.contains("titleimage")) {
          titleimage = parseResult._2("titleimage")
        }
        duration = parseResult._3
        connectError = parseResult._4
        parseError = parseResult._5
      } else if (site.contentjson.trim != "") {
        val parseResult = parseContentJson(contenturl.url, site.contentjson, 0, site.isbrowser, site.isscroll)
        source = parseResult._1
        if (parseResult._2.contains("postdate")) {
          postdate = parseResult._2("postdate")
        }
        if (parseResult._2.contains("title")) {
          title = parseResult._2("title")
        }
        if (parseResult._2.contains("author")) {
          author = parseResult._2("author")
        }
        if (parseResult._2.contains("content")) {
          contentsource = parseResult._2("content")
        }
        if (parseResult._2.contains("tag")) {
          tag = parseResult._2("tag")
        }
        if (parseResult._2.contains("titleimage")) {
          titleimage = parseResult._2("titleimage")
        }
        duration = parseResult._3
        connectError = parseResult._4
        parseError = parseResult._5
      } else {
        errmsg = s"fetchContent error: cuid = $cuid, site listhtml and listjson empty!"
      }
    } else {
      if (cuid > 0) {
        errmsg = s"fetchContent error: cuid = $cuid, contenturl or site not found!"
      }
    }
    var status = 1
    //更新列表页url
    if (errmsg != "") {
      status = 4
    } else if (connectError != "") {
      status = 3
    } else if (parseError != "") {
      status = 4
    } else {
      status = 2
    }
    var imgSeq = Seq[(String, String, String, String)]()
    var contenttext = ""
    var contentabsurl = ""
    var contentlocalurl = ""
    var titleimageid = 0
    var imagefinish = 0
    if (status == 2) {
      //进行内容解析
      val parseContentResult = parseContentSource(contentsource, url, sid)
      imgSeq = parseContentResult._1
      contenttext = parseContentResult._2
      contentabsurl = parseContentResult._3
      contentlocalurl = parseContentResult._4
      parseError = parseContentResult._5
      //进行图片入库
      imgSeq.foreach{ case (urlabs, urlsrc, path, filename) => {
        if (urlabs.trim != "") {
          val insImage = (images returning images.map(_.imgid)) +=
            Image(0, cuid, sid, urlabs, urlsrc, dateline, 0, path, filename, 0, 0, "", 0)
          Await.ready(db.run(insImage), Duration.Inf)
        }
      }}
      if (imgSeq.isEmpty) {
        imagefinish = 1
      }
      if (cuid > 0) {
        log.info(s"fetchContent insert image: cuid = $cuid, image insert count ${imgSeq.length}!")
      }

      //进行titleimage入库
      if (titleimage.trim != "") {
        val datetime = new CurrentDate
        val year = datetime.year
        val month = datetime.month
        val day = datetime.day
        val path = s"upload/$sid/$year$month/$day/"
        val fileName = UUID.randomUUID().toString()
        val insImage = (images returning images.map(_.imgid)) +=
          Image(0, cuid, sid, titleimage, titleimage, dateline, 0, path, fileName, 0, 0, "", 0)
        titleimageid = await{db.run(insImage)}
      }

      //进行tags标签入库
      if (tag.trim != "") {
        val tags = tag.split(",")
        tags.foreach(t => {
          val articleTagQuery = articletags.filter(_.tagname === t).map(_.tid)
          var tid = Await.result(db.run(articleTagQuery.result.headOption), Duration.Inf).getOrElse(0)
          if (tid == 0) {
            val insTag = (articletags returning articletags.map(_.tid)) +=
              ArticleTag(0, t)
            tid = Await.result(db.run(insTag), Duration.Inf)
          }
          val contentTagQuery = contenttags.filter(ct => ct.cuid === cuid && ct.tid === tid).map(_.ctid)
          var ctid = Await.result(db.run(contentTagQuery.result.headOption), Duration.Inf).getOrElse(0)
          if (ctid == 0) {
            val insContentTag = contenttags += ContentTag(0, tid, cuid)
            Await.ready(db.run(insContentTag), Duration.Inf)
          }
        })
      }
    }
    dateline = (new CurrentTimestamp).timestamp
    Await.ready(db.run(
      contenturls.filter(_.cuid === cuid)
        .map(cu => (cu.source, cu.postdate, cu.lastupdate, cu.title, cu.author, cu.contenttext, cu.contentsource, cu.contentabsurl, cu.contentlocalurl, cu.titleimageid, cu.status, cu.imagefinish, cu.connecterror, cu.parseerror, cu.duration))
        .update((source, postdate, dateline, title, author, contenttext, contentsource, contentabsurl, contentlocalurl, titleimageid, status, imagefinish, connectError, parseError, duration))
    ), Duration.Inf)
    if (errmsg != "") {
      log.error(errmsg)
    } else {
      if (cuid > 0) {
        log.info(s"fetchContent success, cuid = $cuid")
      }
    }
  }.recover{
    case e: Throwable =>
      log.error(s"fetchContent error: cuid = $cuid, ${e.getMessage}, ${e.getCause}")
  }

  //定期扫描微信公众号contenturls表,并获取一条符合条件的cuid
  //return: Future[Int, String]: Int:符合条件的cuid, String:错误提示
  def scanWxContent(sid: Int)(implicit log: LoggingAdapter): Future[(Int, String)] = async{
    val errmsg = ""
    val dateline = (new CurrentTimestamp).timestamp
    val contenturlQuery = {
      for {
        cu <- contenturls
        site <- sites
        //注意,这里不扫描微信公众号的文章
        if cu.sid === site.sid && cu.sid === sid && site.isweixin === 1 &&
          //查询contenturs.isenable=1并且(listurs待处理 或者 listurs已处理的isfirst)
          (cu.status === 0 || cu.status === 3) && site.lastfetch+site.interval < dateline
      } yield (cu.cuid, cu.url, site.lastfetch)
    }.sortBy{
      case (cuid, url, lastfetch) => lastfetch.desc
    }.take(1)
    val (cuid, url, lastfetch) = await{db.run(contenturlQuery.result.headOption)}.getOrElse(0, "", 0)
    if (cuid > 0) {
      log.info(s"scanWxContent success: sid = $sid, cuid = $cuid, url = $url")
    }
    (cuid, errmsg)
  }.recover{
    case e: Throwable =>
      val errmsg = s"scanWxContent error: sid = $sid, ${e.getMessage}, ${e.getCause}"
      log.error(errmsg)
      (0, errmsg)
  }

  //爬取指定的微信公众号内容页,更新contenturls表
  def fetchWxContent(cuid: Int)(implicit log: LoggingAdapter): Future[Unit] = async{
    var errmsg = ""
    var dateline = (new CurrentTimestamp).timestamp
    val contenturlQuery = {
      for {
        cu <- contenturls
        site <- sites
        if cu.sid === site.sid && cu.cuid === cuid
      } yield (cu, site)
    }
    val (contenturl, site) = await{db.run(contenturlQuery.result.headOption)}.getOrElse(null, null)
    var url = ""
    var source = ""
    var contenttext = ""
    var contentabsurl = ""
    var contentlocalurl = ""
    var sid = 0
    var postdate = ""
    var title = ""
    var author = ""
    var extrainfo = ""
    var contentsource = ""
    var tag = ""
    var titleimage = ""
    var imgSeq = Seq[(String, String, String, String)]()
    var imagefinish = 0
    var titleimageid = 0
    var duration = 0
    var connectError = ""
    var parseError = ""
    if (contenturl != null && site != null) {
      url = contenturl.url
      sid = contenturl.sid
      //更新sites站点表的lastfetch
      Await.ready(db.run(sites.filter(_.sid === sid).map(_.lastfetch).update(dateline)), Duration.Inf)
      //更新列表页url为待更新状态
      Await.ready(db.run(contenturls.filter(_.cuid === cuid).map(cu => (cu.status, cu.lastupdate)).update((1, dateline))), Duration.Inf)
      val parseResult = parseWxArticle(contenturl.url, site.sid)
      duration = parseResult._3
      connectError = parseResult._4
      parseError = parseResult._5
      if (parseResult._1.nonEmpty) {
        source = parseResult._1("source")
        contentsource = parseResult._1("content")
        contenttext = parseResult._1("contenttext")
        contentabsurl = parseResult._1("contentabsurl")
        contentlocalurl = parseResult._1("contentlocalurl")
        title = parseResult._1("title")
        postdate = parseResult._1("postdate")
        author = parseResult._1("author")
        titleimage = parseResult._1("titleimage")
        tag = ""
        extrainfo = s"阅读: ${parseResult._1("readcount")}, 点赞: ${parseResult._1("likecount")}"
        imgSeq = parseResult._2
        if (imgSeq.isEmpty) {
          imagefinish = 1
        }
      }
    } else {
      if (cuid > 0) {
        errmsg = s"fetchWxContent error: cuid = $cuid, contenturl or site not found!"
      }
    }
    var status = 1
    //更新列表页url
    if (errmsg != "") {
      status = 4
    } else if (connectError != "") {
      status = 3
    } else if (parseError != "") {
      status = 4
    } else {
      status = 2
    }
    if (status == 2) {
      //进行图片入库
      imgSeq.foreach{ case (urlabs, urlsrc, path, filename) => {
        if (urlabs.trim != "") {
          val insImage = (images returning images.map(_.imgid)) +=
            Image(0, cuid, sid, urlabs, urlsrc, dateline, 0, path, filename, 0, 0, "", 0)
          Await.ready(db.run(insImage), Duration.Inf)
        }
      }}
      if (cuid > 0) {
        log.info(s"fetchWxContent insert image: cuid = $cuid, image insert count ${imgSeq.length}!")
      }

      //进行titleimage入库
      if (titleimage.trim != "") {
        val datetime = new CurrentDate
        val year = datetime.year
        val month = datetime.month
        val day = datetime.day
        val path = s"upload/$sid/$year$month/$day/"
        val fileName = UUID.randomUUID().toString()
        val insImage = (images returning images.map(_.imgid)) +=
          Image(0, cuid, sid, titleimage, titleimage, dateline, 0, path, fileName, 0, 0, "", 0)
        titleimageid = await{db.run(insImage)}
      }

      //进行tags标签入库
      if (tag.trim != "") {
        val tags = tag.split(",")
        tags.foreach(t => {
          val articleTagQuery = articletags.filter(_.tagname === t).map(_.tid)
          var tid = Await.result(db.run(articleTagQuery.result.headOption), Duration.Inf).getOrElse(0)
          if (tid == 0) {
            val insTag = (articletags returning articletags.map(_.tid)) +=
              ArticleTag(0, t)
            tid = Await.result(db.run(insTag), Duration.Inf)
          }
          val contentTagQuery = contenttags.filter(ct => ct.cuid === cuid && ct.tid === tid).map(_.ctid)
          var ctid = Await.result(db.run(contentTagQuery.result.headOption), Duration.Inf).getOrElse(0)
          if (ctid == 0) {
            val insContentTag = contenttags += ContentTag(0, tid, cuid)
            Await.ready(db.run(insContentTag), Duration.Inf)
          }
        })
      }
    }
    dateline = (new CurrentTimestamp).timestamp
    Await.ready(db.run(
      contenturls.filter(_.cuid === cuid)
        .map(cu => (cu.source, cu.postdate, cu.lastupdate, cu.author, cu.extrainfo, cu.contenttext, cu.contentsource, cu.contentabsurl, cu.contentlocalurl, cu.titleimageid, cu.status, cu.imagefinish, cu.connecterror, cu.parseerror, cu.duration))
        .update((source, postdate, dateline, author, extrainfo, contenttext, contentsource, contentabsurl, contentlocalurl, titleimageid, status, imagefinish, connectError, parseError, duration))
    ), Duration.Inf)
    if (errmsg != "") {
      log.error(errmsg)
    } else {
      if (cuid > 0) {
        log.info(s"fetchWxContent success, cuid = $cuid")
      }
    }
  }.recover{
    case e: Throwable =>
      log.error(s"fetchWxContent error: cuid = $cuid, ${e.getMessage}, ${e.getCause}")
  }

  //根据指定站点获取图片
  def scanImage(sid: Int)(implicit log: LoggingAdapter) = async {
    val errmsg = ""
    val dateline = (new CurrentTimestamp).timestamp
    val contenturlQuery = images.filter(img => (img.status === 0 || img.status === 3) && img.sid === sid).map(img => (img.imgid, img.urlabs)).take(1)
    val (imgid, url) = await{db.run(contenturlQuery.result.headOption)}.getOrElse(0, "")
//    if (imgid > 0) {
//      log.info(s"scanImage success: sid = $sid, imgid = $imgid, url = $url")
//    }
    (imgid, errmsg)
  }.recover{
    case e: Throwable =>
      val errmsg = s"scanImage error: sid = $sid, ${e.getMessage}, ${e.getCause}"
      log.error(errmsg)
      (0, errmsg)
  }

  //爬取指定的图片,并更新images表
  def fetchImage(imgid: Int)(implicit log: LoggingAdapter): Future[Unit] = async {
    var errmsg = ""
    var dateline = (new CurrentTimestamp).timestamp
    val imageQuery = images.filter(_.imgid === imgid)
    val image = await{db.run(imageQuery.result.headOption)}.getOrElse(null)
    if (image != null) {
      val cuid = image.cuid
      var status = 1
      //更新图片为待更新状态
      Await.ready(db.run(images.filter(_.imgid === imgid).map(img => (img.status, img.lastupdate)).update((status, dateline))), Duration.Inf)
      val (size, duration, connectError) = imageFetch(image.urlabs, image.path, image.filename)

      //更新图片信息
      if (errmsg != "") {
        status = 4
      } else if (connectError != "") {
        status = 3
      } else {
        status = 2
      }
      dateline = (new CurrentTimestamp).timestamp
      Await.ready(db.run(
        images.filter(_.imgid === imgid)
          .map(img => (img.status, img.lastupdate, img.size, img.duration, img.connecterror))
          .update((status, dateline, size, duration, connectError))
      ), Duration.Inf)
      //更新contenturls的imagefinish字段,检查是否有待处理的images
      val preFetchCount = await{db.run(images.filter(img => img.cuid === cuid && img.status === 0).length.result)}
      if (preFetchCount == 0) {
        await{db.run(contenturls.filter(_.cuid === cuid).map(_.imagefinish).update(1))}
      }
    } else {
      errmsg = s"fetchImage error: imgid = $imgid, image not exists"
    }
    if (errmsg != "") {
      log.error(errmsg)
    } else {
      log.info(s"fetchImage success, imgid = $imgid")
    }
  }.recover{
    case e: Throwable =>
      log.error(s"fetchImage error: imgid = $imgid, ${e.getMessage}, ${e.getCause}")
  }

  //把已经完成爬取的contenturl写入ES,每次导入5个
  def scanIndexContent(count: Int)(implicit log: LoggingAdapter) = async {
    val t1 = System.currentTimeMillis
    var insertCount = 0
    val contenturlQuery = contenturls.filter(cu => cu.status === 2 && cu.indexfinish === 0).map(cu => (cu.cuid, cu.url, cu.title, cu.contenttext, cu.lastupdate)).sortBy(cu => cu._1.asc).take(count)
    val contenturlResult = await{db.run(contenturlQuery.result)}
    contenturlResult.foreach{case (cuid, url, title, contenttext, lastupdate) => {
      var tags = ""
      val tagsQuery = {
        for {
          ctag <- contenttags
          atag <- articletags
          if ctag.tid === atag.tid && ctag.cuid === cuid
        } yield atag.tagname
      }
      tags = Await.result(db.run(tagsQuery.result), Duration.Inf).mkString(",")
    }}
    val t2 = System.currentTimeMillis
    val duration = Math.round(t2 - t1)
    if (insertCount > 0) {
      log.info(s"scanIndexContent success, insertCount: $insertCount, duration: $duration")
    }
  }.recover{
    case e: Throwable =>
      log.error(s"scanIndexContent error: ${e.getMessage}, ${e.getCause}")
  }

  //轮询读取sid
  def roundRobinSite(lastSid: Int, isWeixin: Int = -1): Future[Int] = async {
    var sid = 0
    if (isWeixin == -1) {
      val siteQuery = sites.filter(site => site.isenable === 1 && site.sid > lastSid).map(_.sid).take(1)
      sid = await{db.run(siteQuery.result.headOption)}.getOrElse(0)
    } else if (isWeixin == 0 || isWeixin == 1) {
      val siteQuery = sites.filter(site => site.isenable === 1 && site.isweixin === isWeixin && site.sid > lastSid).map(_.sid).take(1)
      sid = await{db.run(siteQuery.result.headOption)}.getOrElse(0)
    }
    if (sid == 0) {
      if (isWeixin == -1) {
        val siteQuery2 = sites.filter(site => site.isenable === 1).sortBy(site => site.sid.asc).map(_.sid).take(1)
        sid = await{db.run(siteQuery2.result.headOption)}.getOrElse(0)
      } else if (isWeixin == 0 || isWeixin == 1) {
        val siteQuery2 = sites.filter(site => site.isenable === 1 && site.isweixin === isWeixin).sortBy(site => site.sid.asc).map(_.sid).take(1)
        sid = await{db.run(siteQuery2.result.headOption)}.getOrElse(0)
      }
    }
    sid
  }
}
