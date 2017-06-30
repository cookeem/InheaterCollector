package com.inheater.collector.http

import com.inheater.collector.common.CommonOps.CurrentTimestamp
import play.api.libs.json._

/**
  * Created by cookeem on 16/4/24.
  */
object ConfigVerify {
  //解释站点数据表的wxconfig字段配置是否正确
  def verifyWxConfig(wxConfig: String): String = {
    var errmsg = ""
    val error = s"verifyWxConfig error: \n$wxConfig"
    try {
      val json = Json.parse(wxConfig)
      val keyword = (json \ "keyword").getOrElse(JsString("")).as[String]
      val contenttype = (json \ "contenttype").getOrElse(JsNumber(0)).as[Int]
      if (keyword == "" || (contenttype != 0 && contenttype != 1)) {
        errmsg = error
      }
      errmsg
    } catch {
      case e: Throwable =>
        val errmsg = s"verifyWxConfig error: ${e.getMessage}, ${e.getCause}"
        errmsg
    }
  }

  //解释站点数据表的urlformat字段配置是否正确,并返回生成的listurl列表
  def verifyUrlFormat(urlFormat: String): (Seq[String], String) = {
    var ret = Seq[String]()
    var errmsg = ""
    val error = s"verifyUrlFormat error: \n$urlFormat"
    try {
      val json = Json.parse(urlFormat)
      val url = (json \ "url").getOrElse(JsString("")).as[String]
      val numfirst = (json \ "numfirst").getOrElse(JsNumber(0)).as[Int]
      val numlast = (json \ "numlast").getOrElse(JsNumber(0)).as[Int]
      val timestamp = (new CurrentTimestamp).timestamp
      if (url != "" && numfirst >= 0 && numlast >= 0 && url.contains("<num>") && (url.startsWith("http://") || url.startsWith("https://"))) {
        ret = (numfirst to numlast).map(i => {
          url.replace("<num>", i.toString).replace("<timestamp>", timestamp.toString)
        })
      } else {
        errmsg = error
      }
      (ret, errmsg)
    } catch {
      case e: Throwable =>
        val errmsg = s"verifyUrlFormat error: ${e.getMessage}, ${e.getCause}"
        (Seq[String](), errmsg)
    }
  }

  //解释站点数据表配置的listjson字段配置是否正确
  def verifyListJson(listJson: String): String = {
    var errmsg = ""
    val error = s"verifyListJson error: \n$listJson"
    try {
      val json = Json.parse(listJson)
      val url = (json \ "contenturl").getOrElse(JsString("")).as[String].trim
      if (url == "") {
        errmsg = error
      }
      val listArr = url.split(" ")
      if (listArr.length % 2 != 0) {
        errmsg = error
      } else {
        for (i <- 0 to (listArr.length - 1)) {
          if (i % 2 == 0) {
            if (listArr(i) != "|" && listArr(i) != "||") {
              errmsg = error
            }
          }
        }
      }
      errmsg
    } catch {
      case e: Throwable =>
        s"verifyListJson error: ${e.getMessage}, ${e.getCause}"
    }
  }

  //解释站点数据表配置的listhtml字段配置是否正确
  def verifyListHtml(listHtml: String): String = {
    var errmsg = ""
    val error = s"verifyListHtml error: \n$listHtml"
    try {
      val json = Json.parse(listHtml)
      val list = (json \ "list").getOrElse(JsString("")).as[String]
      val url = (json \ "contenturl").getOrElse(JsString("")).as[String]
      if (list == "" || url == "") {
        errmsg = error
      }
      errmsg
    } catch {
      case e: Throwable =>
        s"verifyListHtml error: ${e.getMessage}, ${e.getCause}"
    }
  }

  //解释站点数据表配置的contentjson字段配置是否正确
  def verifyContentJson(contentJson: String): String = {
    var errmsg = ""
    val error = s"verifyContentJson error: \n$contentJson"
    try {
      val json = Json.parse(contentJson)
      //包含的内容,true表示必须提供,false表示可选
      val formatMap =
        Map(
          "content" -> true,
          "title" -> true,
          "author" -> false,
          "postdate" -> false,
          "tag" -> false,
          "titleimage" -> false
        )
      formatMap.foreach{case (k,v) => {
        val element = (json \ k).getOrElse(JsString("")).as[String].trim
        if (v && element == "") {
          errmsg = error
        } else if (element != "") {
          val listArr = element.split(" ")
          if (listArr.length % 2 != 0) {
            errmsg = error
          } else {
            for (i <- 0 to (listArr.length - 1)) {
              if (i % 2 == 0) {
                if (listArr(i) != "|" && listArr(i) != "||") {
                  errmsg = error
                }
              }
            }
          }
        }
      }}
      errmsg
    } catch {
      case e: Throwable =>
        s"verifyContentJson error: ${e.getMessage}, ${e.getCause}"
    }
  }

  //解释站点数据表配置的contenthtml字段配置是否正确
  def verifyContentHtml(contentHtml: String): String = {
    var errmsg = ""
    val error = s"verifyContentHtml error: \n$contentHtml"
    try {
      val json = Json.parse(contentHtml)
      //包含的内容,true表示必须提供,false表示可选
      val formatMap =
        Map(
          "content" -> true,
          "title" -> true,
          "author" -> false,
          "postdate" -> false,
          "tag" -> false,
          "titleimage" -> false
        )
      formatMap.foreach{case (k,v) => {
        val element = (json \ k).getOrElse(JsString("")).as[String]
        if (v && element == "") {
          errmsg = error
        }
      }}
      errmsg
    } catch {
      case e: Throwable =>
        s"verifyContentHtml error: ${e.getMessage}, ${e.getCause}"
    }
  }


}
