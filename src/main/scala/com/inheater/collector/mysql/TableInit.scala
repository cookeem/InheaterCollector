package com.inheater.collector.mysql

import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable
import Tables._

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import scala.collection.JavaConversions._

/**
  * Created by cookeem on 16/4/23.
  */
object TableInit {
  def onTableInit: Future[Int] = {
    val futureTableInit = async {
      val tables = await{db.run(MTable.getTables)}.map(mt => mt.name.name.toLowerCase())
      //初始化所有数据表
      if (!tables.contains("sites")) {
        val sitesSetupQuery = DBIO.seq(
          sites.schema.create
        )
        await{db.run(sitesSetupQuery)}
      }
      if (!tables.contains("listurls")) {
        val listurlsSetupQuery = DBIO.seq(
          listurls.schema.create
        )
        await{db.run(listurlsSetupQuery)}
      }
      if (!tables.contains("contenturls")) {
        val contenturlsSetupQuery = DBIO.seq(
          contenturls.schema.create
        )
        await{db.run(contenturlsSetupQuery)}
      }
      if (!tables.contains("images")) {
        val imagesSetupQuery = DBIO.seq(
          images.schema.create
        )
        await{db.run(imagesSetupQuery)}
      }
      if (!tables.contains("articletags")) {
        val articletagsSetupQuery = DBIO.seq(
          articletags.schema.create
        )
        await{db.run(articletagsSetupQuery)}
      }
      if (!tables.contains("contenttags")) {
        val contenttagsSetupQuery = DBIO.seq(
          contenttags.schema.create
        )
        await{db.run(contenttagsSetupQuery)}
      }
      val listurlUpdate = listurls.filter(_.status === 1).map(_.status).update(0)
      await{db.run(listurlUpdate)}
      val contenturlStatusUpdate = contenturls.filter(_.status === 1).map(_.status).update(0)
      await{db.run(contenturlStatusUpdate)}
      val contenturlIndexUpdate = contenturls.filter(_.indexfinish === 1).map(_.indexfinish).update(0)
      await{db.run(contenturlIndexUpdate)}
      val imageUpdate = images.filter(_.status === 1).map(_.status).update(0)
      await{db.run(imageUpdate)}

      println(s"MysqlDB initial finish!")
      1
    }.recover{
      case e: Throwable =>
        println(s"MysqlDB initial error: ${e.getMessage}, ${e.getCause}")
        0
    }
    futureTableInit
  }
}
