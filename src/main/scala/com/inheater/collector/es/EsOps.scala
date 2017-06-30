package com.inheater.collector.es

import java.io.File
import java.net.InetAddress

import com.typesafe.config.ConfigFactory
import org.ansj.splitWord.analysis.NlpAnalysis
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.action.termvectors.TermVectorsRequest
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.{XContentBuilder, ToXContent, XContentFactory}
import org.elasticsearch.search.sort.{SortOrder, SortBuilders}
import org.nlpcn.commons.lang.pinyin.Pinyin
import play.api.libs.json.{JsObject, Json}

import scala.collection.JavaConversions._

/**
  * Created by cookeem on 16/5/12.
  */
object EsOps {
  var client: TransportClient = null
  val config =  ConfigFactory.parseFile(new File("conf/application.conf"))
  val esClusterName = config.getString("elasticsearch.clustername")
  val esHosts = config.getConfigList("elasticsearch.hosts")
  val numberOfShards = config.getInt("elasticsearch.shards")
  val numberOfReplicas = config.getInt("elasticsearch.replicas")
  val indexName = "webcollector"
  val typeName = "contenturls"

  try {
    val settings = Settings
      .settingsBuilder()
      .put("cluster.name", esClusterName).build()
    client = TransportClient
      .builder()
      .settings(settings)
      .build()
    esHosts.foreach(cfg => {
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(cfg.getString("hostname")), cfg.getInt("port")))
    })
  } catch {
    case e: Throwable =>
      println(s"elasticsearch connect error: ${e.getMessage}, ${e.getCause}")
  }

  //创建client


  initIndex()

  //检查索引是否存在,不存在则创建索引
  //return: Future(String:errmsg)
  def initIndex(): String = {
    var errmsg = ""
    try {
      if (!client.admin().indices().prepareExists(indexName).execute().actionGet().isExists) {
        val indexMapping = client.admin().indices().prepareCreate(indexName)
          .setSettings(
            XContentFactory.jsonBuilder()
              .startObject()
                .field("number_of_shards", numberOfShards)
                .field("number_of_replicas", numberOfReplicas)
                .startObject("analysis")
                  .startObject("filter")
                    .startObject("keep_word_types")
                      .field("type", "keep_types")
                      .startArray("types")
                        .value("n")
                        .value("nr")
                        .value("nr1")
                        .value("nr2")
                        .value("nrj")
                        .value("nrf")
                        .value("ns")
                        .value("nsf")
                        .value("nt")
                        .value("nz")
                        .value("nl")
                        .value("ng")
                        .value("nw")
                        .value("v")
                        .value("vd")
                        .value("vn")
                        .value("vf")
                        .value("vx")
                        .value("vi")
                        .value("vl")
                        .value("vg")
                        .value("a")
                        .value("ad")
                        .value("an")
                        .value("ag")
                        .value("al")
                      .endArray()
                    .endObject()
                    .startObject("whitespace_remove")
                      .field("type", "pattern_replace")
                      .field("pattern", " ")
                      .field("replacement", "")
                    .endObject()
                  .endObject()
                  .startObject("analyzer")
                    .startObject("index_lowercase_ansj")
                      .field("tokenizer", "index_ansj")
                      .startArray("filter")
                        .value("lowercase")
                        .value("whitespace_remove")
                      .endArray()
                    .endObject()
                    .startObject("index_pinyin_ansj")
                        .field("tokenizer", "index_ansj")
                        .startArray("filter")
                            .value("pinyin")
                            .value("whitespace_remove")
                            .value("lowercase")
                        .endArray()
                    .endObject()
                    .startObject("index_pinyinfirst_ansj")
                      .field("tokenizer", "index_ansj")
                      .startArray("filter")
                        .value("pinyin_first_letter")
                        .value("whitespace_remove")
                        .value("lowercase")
                      .endArray()
                    .endObject()
                    .startObject("index_keeptypes_ansj")
                      .field("tokenizer", "index_ansj")
                      .startArray("filter")
                        .value("lowercase")
                        .value("whitespace_remove")
                        .value("keep_word_types")
                      .endArray()
                    .endObject()
                    .startObject("query_lowercase_ansj")
                      .field("tokenizer", "query_ansj")
                      .startArray("filter")
                        .value("lowercase")
                        .value("whitespace_remove")
                      .endArray()
                    .endObject()
                    .startObject("query_pinyin_ansj")
                      .field("tokenizer", "query_ansj")
                      .startArray("filter")
                        .value("pinyin")
                        .value("whitespace_remove")
                        .value("lowercase")
                      .endArray()
                    .endObject()
                    .startObject("query_pinyinfirst_ansj")
                      .field("tokenizer", "query_ansj")
                        .startArray("filter")
                          .value("pinyin_first_letter")
                          .value("whitespace_remove")
                          .value("lowercase")
                        .endArray()
                    .endObject()
                  .endObject()
                .endObject()
              .endObject()
          )
          .addMapping(typeName,
            XContentFactory.jsonBuilder()
              .startObject()
                .startObject(typeName)
                  .startObject("properties")
                    .startObject("cuid")
                      .field("type", "integer")
                    .endObject()
                    .startObject("url")
                      .field("type", "string")
                      .field("index", "not_analyzed")
                    .endObject()
                    .startObject("title")
                      .field("type","string")
                      .startObject("fields")
                        .startObject("raw")
                          .field("type", "string")
                          .field("analyzer", "index_lowercase_ansj")
                          .field("search_analyzer", "query_lowercase_ansj")
//                          .field("term_vector", "with_positions_offsets_payloads")
                        .endObject()
                        .startObject("pinyin")
                          .field("type", "string")
                          .field("analyzer", "index_pinyin_ansj")
                          .field("search_analyzer", "query_pinyin_ansj")
                        .endObject()
                        .startObject("pinyinfirst")
                          .field("type", "string")
                          .field("analyzer", "index_pinyinfirst_ansj")
                          .field("search_analyzer", "query_pinyinfirst_ansj")
                        .endObject()
                      .endObject()
                    .endObject()
                    .startObject("tags")
                      .field("type","string")
                      .startObject("fields")
                        .startObject("raw")
                          .field("type", "string")
                          .field("analyzer", "index_lowercase_ansj")
                          .field("search_analyzer", "query_lowercase_ansj")
//                          .field("term_vector", "with_positions_offsets_payloads")
                        .endObject()
                        .startObject("pinyin")
                          .field("type", "string")
                          .field("analyzer", "index_pinyin_ansj")
                          .field("search_analyzer", "query_pinyin_ansj")
                        .endObject()
                        .startObject("pinyinfirst")
                          .field("type", "string")
                          .field("analyzer", "index_pinyinfirst_ansj")
                          .field("search_analyzer", "query_pinyinfirst_ansj")
                        .endObject()
                      .endObject()
                    .endObject()
                    .startObject("contenttext")
                      .field("type","string")
                      .startObject("fields")
                        .startObject("raw")
                          .field("type", "string")
                          .field("analyzer", "index_lowercase_ansj")
                          .field("search_analyzer", "query_lowercase_ansj")
//                          .field("term_vector", "with_positions_offsets_payloads")
                        .endObject()
                        .startObject("keyword")
                          .field("type", "string")
                          .field("analyzer", "index_keeptypes_ansj")
                          .field("search_analyzer", "query_lowercase_ansj")
                          .field("term_vector", "with_positions_offsets_payloads")
                        .endObject()
                        .startObject("pinyin")
                          .field("type", "string")
                          .field("analyzer", "index_pinyin_ansj")
                          .field("search_analyzer", "query_pinyin_ansj")
                        .endObject()
                        .startObject("pinyinfirst")
                          .field("type", "string")
                          .field("analyzer", "index_pinyinfirst_ansj")
                          .field("search_analyzer", "query_pinyinfirst_ansj")
                        .endObject()
                      .endObject()
                    .endObject()
                  .endObject()
                .endObject()
              .endObject()
          )
        val indexMappingResponse = indexMapping.execute().actionGet()
      }
    } catch {
      case e: Throwable =>
        errmsg = s"initIndex error: ${e.getMessage}, ${e.getCause}"
    }
    errmsg
  }

  //删除所有index
  //return: Future(String:errmsg)
  def removeAllIndex(): String = {
    var errmsg = ""
    try {
      client.admin().indices().prepareGetIndex().execute().actionGet().getIndices.foreach(idxName => {
        client.admin().indices().prepareDelete(idxName).execute().actionGet()
      })
    } catch {
      case e: Throwable =>
        errmsg = s"removeAllIndex error: ${e.getMessage}, ${e.getCause}"
    }
    errmsg
  }

  //索引contenturls
  //return: Future(String:indexId, String:errmsg)
  def indexContentUrls(cuid: Int, url: String, title: String, tags: String, contenttext: String): (String, String) = {
    var idxId = ""
    var errmsg = ""
    try {
      if (client.admin().indices().prepareExists(indexName).execute().actionGet().isExists) {
        val indexResponse = client.prepareIndex(indexName, typeName)
          .setSource(
            XContentFactory.jsonBuilder()
              .startObject()
                .field("cuid", cuid)
                .field("url", url)
                .field("title", title)
                .field("tags", tags)
                .field("contenttext", contenttext)
              .endObject()
          ).get()
        idxId = indexResponse.getId
      }
    } catch {
      case e: Throwable =>
        errmsg = s"indexContentUrls error: cuid = $cuid, ${e.getMessage}, ${e.getCause}"
    }
    (idxId, errmsg)
  }

  def extractKeywords() = {
/*
    curl -XGET 'localhost:9200/webcollector/contenturls/_termvectors?&pretty' -d '
    {
      "doc": {
        "contenttext": "今年台北国际电脑展创新设计奖已经出炉"
      },
      "term_statistics" : true,
      "field_statistics" : true,
      "dfs": true,
      "positions": false,
      "offsets": false,
      "filter" : {
        "max_num_terms" : 5,
        "min_term_freq" : 1,
        "min_doc_freq" : 1
      }
    }'
*/
    val str = "不光纽扣电池 还有多种异物 开头先说点别的！上周著名制片人方励老师在直播平台上那一跪，想你应该知道了吧？为什么跪？因为《百鸟朝凤》"
    val id = "AVVCln4ImWWaWQH-CMbF"
    val tvs = new TermVectorsRequest.FilterSettings()
    tvs.maxNumTerms = 5
    tvs.minTermFreq = 1
    tvs.minDocFreq = 1
    val tvsResult = client.prepareTermVectors(indexName, typeName, id).setDoc(
      XContentFactory.jsonBuilder()
        .startObject()
        .field("contenttext", str)
        .endObject()
      )
      .setFieldStatistics(true)
      .setFieldStatistics(true)
      .setDfs(true)
      .setPositions(false)
      .setOffsets(false)
      .setFilterSettings(tvs)
      .execute()
      .actionGet()
    val builder = XContentFactory.jsonBuilder().startObject()
    tvsResult.toXContent(builder, ToXContent.EMPTY_PARAMS)
    builder.endObject()
    val jsonStr = builder.string()
    val json = Json.parse(jsonStr)
    val keyword = (json \ "term_vectors" \ "contenttext.keyword" \ "terms").get.as[JsObject].keys
    keyword.foreach(println)
  }

  def searchContenturls() = {
/*
    curl -XGET 'http://localhost:9200/webcollector/contenturls/_search?pretty' -d '{
      "query" : {
        "term" : { "title.pinyinfirst" : "sj" }
      }
    }
    '

    curl -XGET 'http://localhost:9200/webcollector/contenturls/_search?pretty' -d '{
        "query" : {
            "term" : { "title.pinyin" : "shoujia" }
        }
    }
    '
*/
    //生成拼音
    val str = "S第一次T知道LifeStraw系列 产品是在今年二月的ISPO户外展上，当时LifeStraw的展台人满为患，隔着人群看了几眼，实验者用生命吸管从充满泥土和枯枝烂叶的污水中喝水。当时的感觉这个产品真厉害，因为还没有上市，觉得这个玩意一定很贵，不适合我这样户外菜鸟使用。Google"+"完"
    NlpAnalysis.parse(str)
    val pinyin = Pinyin.pinyin(str)
    val pinyinList = pinyin.filter(_ != null) ++ str.split("\\W+")
    val pinyinCaptialList = pinyin.filter(_ != null).map(s => s(0)) ++ str.split("\\W+")

    val queryStr = "meiguo"
    val response = client.prepareSearch(indexName)
      .setTypes(typeName)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
      .setQuery(multiMatchQuery(queryStr, "title.raw", "title.pinyin", "title.pinyinfirst"))
      .setFrom(0)
      .setSize(10)
      .addSort("title", SortOrder.ASC)
      .addSort(SortBuilders.scoreSort())
      .addFields("title")
      .execute()
      .actionGet()
    response.getHits.foreach(sh => {
      val s = sh.getSourceAsString
      println(sh.field("title").value())
    })
  }

}
