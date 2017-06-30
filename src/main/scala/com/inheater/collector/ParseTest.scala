package com.inheater.collector

import com.inheater.collector.http.ConfigVerify._
import com.inheater.collector.mysql.Tables._
import com.inheater.collector.http.SourceParser._

import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by cookeem on 16/5/9.
  */

object ParseTest extends App {
  var prompt = "Usage: com.inheater.collector.ParseTest <siteid>"
  if (args.length != 1) {
    println(prompt)
  } else {
    try {
      val sid = args(0).toInt
      val siteQuery = sites.filter(_.sid === sid)
      val site = Await.result(db.run(siteQuery.result.headOption), Duration.Inf).orNull
      if (site != null) {
        val vuf = verifyUrlFormat(site.urlformat)
        println(s"urlformat #############################")
        vuf._1.foreach(println)

        vuf._1.take(2).foreach(listurl => {
          var urls = Seq[String]()
          if (site.listjson.trim().length > 10) {
            println(s"listjson $listurl #############################")
            val plj: (String, Seq[String], Int, String, String) = parseListJson(listurl, site.listjson, 0, site.isbrowser, site.isscroll)
            plj._2.foreach(println)
            urls = plj._2.take(5).seq
          }

          if (site.listhtml.trim().length > 10) {
            println(s"listhtml $listurl #############################")
            val plh: (String, Seq[String], Int, String, String) = parseListHtml(listurl, site.listhtml, 0, site.isbrowser, site.isscroll)
            plh._2.foreach(println)
            urls = plh._2.take(5).seq
          }

          urls.foreach(url => {
            if (site.contentjson.trim.length > 10) {
              println(s"contentjson $url #############################")
              val pcj = parseContentJson(url, site.contentjson, 0, site.isbrowser, site.isscroll)
              pcj._2.foreach{case (k,v) =>
                if (k == "content") {
                  println(s"$k -> ${v.substring(0, 100)}")
                } else {
                  println(s"$k -> $v")
                }
              }
            }

            if (site.contenthtml.trim.length > 10) {
              println(s"contenthtml $url #############################")
              val pch = parseContentHtml(url, site.contenthtml, 0, site.isbrowser, site.isscroll)
              pch._2.foreach{case (k,v) =>
                if (k == "content") {
                  println(s"$k -> ${v.substring(0, 100)}")
                } else {
                  println(s"$k -> $v")
                }
              }
            }
          })

          Thread.sleep(5000)
        })
      } else {
        println(s"@@@ Error siteid not exists: sid = $sid")
      }
    } catch {
      case e: Throwable =>
        println("Usage: com.inheater.collector.ParseTest <siteid>, <siteid> must int type")
    }
  }

}
