package com.inheater.collector.mysql

import java.io.File

import com.typesafe.config.ConfigFactory
import slick.driver.MySQLDriver.api._

/**
  * Created by cookeem on 16/4/22.
  */
object Tables {

  //站点数据表
  case class Site(sid: Int, sitename: String, siteurl: String, entryurl: String, urlformat: String, isenable: Int, isweixin: Int, wxconfig: String, isbrowser: Int, isscroll: Int, islocal: Int, listjson: String, listhtml: String, contentjson: String, contenthtml: String, interval: Int, schedule: Int, lastfetch: Int, parseerror: String, listcreated: Int)
  class Sites(tag: Tag) extends Table[Site](tag, "sites") {
    def sid = column[Int]("sid", O.AutoInc, O.PrimaryKey)   //站点ID
    def sitename = column[String]("sitename", O.Default(""), O.Length(50))    //站点名称
    def siteurl = column[String]("siteurl", O.Default(""), O.Length(500))    //网站链接
    def entryurl = column[String]("entryurl", O.Default(""), O.Length(500))   //入口页面url，假如入口页面url不为空，那么需要抓取entryurl然后抓取listurl
    def urlformat = column[String]("urlformat", O.Default(""), O.Length(1000))   //抓取列表URL格式,json格式,包含url,numfirst,numlast字段
    def isenable = column[Int]("isenable", O.Default(0))    //是否有效,无效不进行爬取
    def isweixin = column[Int]("isweixin", O.Default(0))    //是否微信公众号爬虫
    def wxconfig = column[String]("wxconfig", O.Default(""), O.Length(1000))   //微信公众号配置
    def isbrowser = column[Int]("isbrowser", O.Default(0))    //是否使用浏览器抓取
    def isscroll = column[Int]("isscroll", O.Default(0))    //是否进行浏览器自动滚屏
    def islocal = column[Int]("islocal", O.Default(0))    //是否必须本地图片才能浏览
    def listjson = column[String]("listjson", O.Default(""), O.Length(1000))   //列表页json数据url所在位置的格式，如果不为空表示格式为json，用于抓取内容url
    def listhtml = column[String]("listhtml", O.Default(""), O.Length(1000))   //列表页html数据url所在位置的格式，如果不为空表示格式为html，用于抓取内容url
    def contentjson = column[String]("contentjson", O.Default(""), O.Length(1000))   //列表页json数据url所在位置的格式，如果不为空表示格式为json，用于抓取内容url，标题，标题图片
    def contenthtml = column[String]("contenthtml", O.Default(""), O.Length(1000))   //内容页html数据内容所在位置的格式，如果不为空表示格式为html，用于抓取内容，标题，标题图片，图片
    def interval = column[Int]("interval", O.Default(20))    //页面抓取时间间隔，单位秒。抓取完第一页，下一页抓取的时间间隔
    def schedule = column[Int]("schedule", O.Default(3600))    //完整爬取完第一次所有列表页之后,重新爬取第一页的时间间隔
    def lastfetch = column[Int]("lastfetch", O.Default(0))    //列表页或者内容页抓取的最后时间戳
    def parseerror = column[String]("parseerror", O.Default(""), O.Length(1000))   //检查listjson、listhtml、contentjson、contenthtml是否存在配置解释错误
    def listcreated = column[Int]("listcreated", O.Default(0))    //列表页是否已经生成。如果urlformat有更新，请务必设置该字段为0

    def * = (sid, sitename, siteurl, entryurl, urlformat, isenable, isweixin, wxconfig, isbrowser, isscroll, islocal, listjson, listhtml, contentjson, contenthtml, interval, schedule, lastfetch, parseerror, listcreated) <> ((Site.apply _).tupled, Site.unapply _)
  }
  lazy val sites = TableQuery[Sites]

  //列表页url
  case class ListUrl(luid: Int, sid: Int, listurl: String, source: String, dateline: Int, lastupdate: Int, connectcount: Int, parsecount: Int, isfirst: Int, isenable: Int, status: Int, connecterror: String, parseerror: String, duration: Int)
  class ListUrls(tag: Tag) extends Table[ListUrl](tag, "listurls") {
    def luid = column[Int]("luid", O.AutoInc, O.PrimaryKey)   //列表页ID
    def sid = column[Int]("sid", O.Default(0))    //对应的站点ID
    def listurl = column[String]("listurl", O.Default(""), O.Length(500))   //列表页URL
    def source = column[String]("source", O.SqlType("MEDIUMTEXT"))   //列表页源码
    def dateline = column[Int]("dateline", O.Default(0))    //生成时间
    def lastupdate = column[Int]("lastupdate", O.Default(0))    //最后更新时间
    def connectcount = column[Int]("connectcount", O.Default(0))    //成功连接次数
    def parsecount = column[Int]("parsecount", O.Default(0))    //成功解析次数
    def isfirst = column[Int]("isfirst", O.Default(0))    //是否列表的第一页，如果是第一页每次都抓取
    def isenable = column[Int]("isenable", O.Default(0))    //是否有效，假如无效不进行爬取
    def status = column[Int]("status", O.Default(0))    //处理状态（0：待处理，1：处理中表示提交到actor，2：已完成，3：下载异常，4：解释异常）
    def connecterror = column[String]("connecterror", O.Default(""), O.Length(1000))   //下载异常原因
    def parseerror = column[String]("parseerror", O.Default(""), O.Length(1000))   //解析异常原因
    def duration = column[Int]("duration", O.Default(0))    //抓取和解析时间

    def * = (luid, sid, listurl, source, dateline, lastupdate, connectcount, parsecount, isfirst, isenable, status, connecterror, parseerror, duration) <> ((ListUrl.apply _).tupled, ListUrl.unapply _)
    def idx_isfirst = index("idx_isfirst", (isfirst), unique = false)
    def idx = index("idx", (isenable, status), unique = false)
  }
  lazy val listurls = TableQuery[ListUrls]

  //内容页
  case class ContentUrl(cuid: Int, luid: Int, sid: Int, url: String, source: String, postdate: String, dateline: Int, lastupdate: Int, title: String, author: String, extrainfo: String, contenttext: String, contentsource: String, contentabsurl: String, contentlocalurl: String, titleimageid: Int, indexfinish: Int, imagefinish: Int, status: Int, connecterror: String, parseerror: String, duration: Int)
  class ContentUrls(tag: Tag) extends Table[ContentUrl](tag, "contenturls") {
    def cuid = column[Int]("cuid", O.AutoInc, O.PrimaryKey)   //内容页ID
    def luid = column[Int]("luid", O.Default(0))    //对应的列表页ID
    def sid = column[Int]("sid", O.Default(0))    //对应的站点ID
    def url = column[String]("url", O.Default(""), O.Length(500))   //内容页URL
    def source = column[String]("source", O.SqlType("MEDIUMTEXT"))   //内容页完整源码
    def postdate = column[String]("postdate", O.Default(""), O.Length(50))   //内容发布时间
    def dateline = column[Int]("dateline", O.Default(0))    //生成时间
    def lastupdate = column[Int]("lastupdate", O.Default(0))    //最后更新时间
    def title = column[String]("title", O.Default(""), O.Length(100))   //标题
    def author = column[String]("author", O.Default(""), O.Length(50))   //作者
    def extrainfo = column[String]("extrainfo", O.Default(""), O.Length(1000))   //其他扩展信息
    def contenttext = column[String]("contenttext", O.SqlType("MEDIUMTEXT"))   //内容文本
    def contentsource = column[String]("contentsource", O.SqlType("MEDIUMTEXT"))   //内容区源码
    def contentabsurl = column[String]("contentabsurl", O.SqlType("MEDIUMTEXT"))   //修改为绝对路径的内容区源码
    def contentlocalurl = column[String]("contentlocalurl", O.SqlType("MEDIUMTEXT"))   //修改为本地图片路径的内容区源码
    def titleimageid = column[Int]("titleimageid", O.Default(0))    //标题图片id
    def indexfinish = column[Int]("indexfinish", O.Default(0))    //是否已经在elasticsearch进行索引（0：待处理，1：处理中，2：已完成，3：处理异常）
    def imagefinish = column[Int]("imagefinish", O.Default(0))    //图片是否下载完成
    def status = column[Int]("status", O.Default(0))    //处理状态（0：待处理，1：处理中表示提交到actor，2：已完成，3：下载异常，4：解释异常）
    def connecterror = column[String]("connecterror", O.Default(""), O.Length(1000))   //下载异常原因
    def parseerror = column[String]("parseerror", O.Default(""), O.Length(1000))   //解析异常原因
    def duration = column[Int]("duration", O.Default(0))    //抓取和解析时间

    def * = (cuid, luid, sid, url, source, postdate, dateline, lastupdate, title, author, extrainfo, contenttext, contentsource, contentabsurl, contentlocalurl, titleimageid, indexfinish, imagefinish, status, connecterror, parseerror, duration) <> ((ContentUrl.apply _).tupled, ContentUrl.unapply _)
  }
  lazy val contenturls = TableQuery[ContentUrls]

  //下载图片
  case class Image(imgid: Int, cuid: Int, sid: Int, urlabs: String, urlsrc: String, dateline: Int, lastupdate: Int, path: String, filename: String, size: Long, status: Int, connecterror: String, duration: Int)
  class Images(tag: Tag) extends Table[Image](tag, "images") {
    def imgid = column[Int]("imgid", O.AutoInc, O.PrimaryKey)   //图片ID
    def cuid = column[Int]("cuid", O.Default(0))    //对应的内容页ID
    def sid = column[Int]("sid", O.Default(0))    //对应的站点ID
    def urlabs = column[String]("urlabs", O.Default(""), O.Length(500))   //图片完整URL
    def urlsrc = column[String]("urlsrc", O.Default(""), O.Length(500))   //图片源URL
    def dateline = column[Int]("dateline", O.Default(0))    //生成时间
    def lastupdate = column[Int]("lastupdate", O.Default(0))    //最后更新时间
    def path = column[String]("path", O.Default(""), O.Length(100))   //本地目录路径
    def filename = column[String]("filename", O.Default(""), O.Length(100))   //文件名
    def size = column[Long]("size", O.Default(0))    //文件大小
    def status = column[Int]("status", O.Default(0))    //处理状态（0：待处理，1：处理中表示提交到actor，2：已完成，3：下载异常，4：解释异常）
    def connecterror = column[String]("connecterror", O.Default(""), O.Length(1000))   //下载异常原因
    def duration = column[Int]("duration", O.Default(0))    //抓取和解析时间

    def * = (imgid, cuid, sid, urlabs, urlsrc, dateline, lastupdate, path, filename, size, status, connecterror, duration) <> ((Image.apply _).tupled, Image.unapply _)
    def idx_cuid = index("idx_cuid", (cuid), unique = false)
    def idx_sid = index("idx_sid", (sid), unique = false)
  }
  lazy val images = TableQuery[Images]

  //标签
  case class ArticleTag(tid: Int, tagname: String)
  class ArticleTags(tag: Tag) extends Table[ArticleTag](tag, "articletags") {
    def tid = column[Int]("tid", O.AutoInc, O.PrimaryKey)   //标签ID
    def tagname = column[String]("tagname", O.Default(""), O.Length(50))  //标签
    def * = (tid, tagname) <> ((ArticleTag.apply _).tupled, ArticleTag.unapply _)
    def idx_tagname = index("idx_tagname", (tagname), unique = true)
  }
  lazy val articletags = TableQuery[ArticleTags]

  //内容页标签对应表
  case class ContentTag(ctid: Int, tid: Int, cuid: Int)
  class ContentTags(tag: Tag) extends Table[ContentTag](tag, "contenttags") {
    def ctid = column[Int]("ctid", O.AutoInc, O.PrimaryKey)   //自动编号ID
    def tid = column[Int]("tid", O.Default(0))    //标签ID
    def cuid = column[Int]("cuid", O.Default(0))    //内容页ID
    def * = (ctid, tid, cuid) <> ((ContentTag.apply _).tupled, ContentTag.unapply _)
    def idx = index("idx", (tid, cuid), unique = false)
  }
  lazy val contenttags = TableQuery[ContentTags]

  val config =  ConfigFactory.parseFile(new File("conf/application.conf"))
  lazy val db = Database.forConfig("mysqlDB", config)

}