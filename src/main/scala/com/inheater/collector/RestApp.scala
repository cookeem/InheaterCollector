package com.inheater.collector

import java.io.File

import com.inheater.collector.common.CommonOps._
import com.inheater.collector.rest.RestQuery._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

/**
  * Created by cookeem on 16/4/28.
  */
object RestApp extends App {
  val config =  ConfigFactory.parseFile(new File("conf/application.conf"))
  val httpPort = config.getInt("http.port")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  def badRequest(request: HttpRequest): StandardRoute = {
    val method = request.method.value.toLowerCase
    val path = request.getUri().path()
    val queryString = request.getUri().rawQueryString().orElse("")
    method match {
      case _ =>
        complete {
            s"request resource not found"
        }
    }
  }

  val route =
    get {
      pathSingleSlash {
        redirect("inheater/index.html", StatusCodes.PermanentRedirect)
      } ~
      pathPrefix("inheater" / "upload") {
        getFromDirectory("upload")
      } ~
      pathPrefix("inheater") {
        getFromDirectory("www/inheater")
      } ~
      //解释json
      path("json" / "siteList") {
        parameterMap { params =>
          complete{
            listSiteQuery()
          }
        }
      } ~
      path("json" / "contentList") {
        parameterMap { params =>
          val page = strToInt(params, "page", 1)
          val count = strToInt(params, "count", 10)
          val sid = strToInt(params, "sid", 0)
          complete{
            listContentQuery(sid, page, count)
          }
        }
      } ~
      path("json" / "content") {
        parameterMap { params =>
          val cuid = strToInt(params, "cuid", 0)
          complete{
            //complete中可以直接放future
            contentQuery(cuid)
          }
        }
      }
    } ~ extractRequest { request =>
      badRequest(request)
    }
  Http().bindAndHandle(route, "localhost", httpPort)
}
