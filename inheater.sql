/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50172
 Source Host           : localhost
 Source Database       : inheater

 Target Server Type    : MySQL
 Target Server Version : 50172
 File Encoding         : utf-8

 Date: 05/11/2016 14:08:07 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `articletags`
-- ----------------------------
DROP TABLE IF EXISTS `articletags`;
CREATE TABLE `articletags` (
  `tid` int(11) NOT NULL AUTO_INCREMENT,
  `tagname` varchar(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`tid`),
  UNIQUE KEY `idx_tagname` (`tagname`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `contenttags`
-- ----------------------------
DROP TABLE IF EXISTS `contenttags`;
CREATE TABLE `contenttags` (
  `ctid` int(11) NOT NULL AUTO_INCREMENT,
  `tid` int(11) NOT NULL DEFAULT '0',
  `cuid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ctid`),
  KEY `idx` (`tid`,`cuid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `contenturls`
-- ----------------------------
DROP TABLE IF EXISTS `contenturls`;
CREATE TABLE `contenturls` (
  `cuid` int(11) NOT NULL AUTO_INCREMENT,
  `luid` int(11) NOT NULL DEFAULT '0',
  `sid` int(11) NOT NULL DEFAULT '0',
  `url` varchar(500) NOT NULL DEFAULT '',
  `source` mediumtext NOT NULL,
  `postdate` varchar(50) NOT NULL DEFAULT '',
  `dateline` int(11) NOT NULL DEFAULT '0',
  `lastupdate` int(11) NOT NULL DEFAULT '0',
  `title` varchar(100) NOT NULL DEFAULT '',
  `author` varchar(50) NOT NULL DEFAULT '',
  `extrainfo` varchar(1000) NOT NULL DEFAULT '',
  `contenttext` mediumtext NOT NULL,
  `contentsource` mediumtext NOT NULL,
  `contentabsurl` mediumtext NOT NULL,
  `contentlocalurl` mediumtext NOT NULL,
  `titleimageid` int(11) NOT NULL DEFAULT '0',
  `indexfinish` int(11) NOT NULL DEFAULT '0',
  `imagefinish` int(11) NOT NULL DEFAULT '0',
  `status` int(11) NOT NULL DEFAULT '0',
  `connecterror` varchar(1000) NOT NULL DEFAULT '',
  `parseerror` varchar(1000) NOT NULL DEFAULT '',
  `duration` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`cuid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `images`
-- ----------------------------
DROP TABLE IF EXISTS `images`;
CREATE TABLE `images` (
  `imgid` int(11) NOT NULL AUTO_INCREMENT,
  `cuid` int(11) NOT NULL DEFAULT '0',
  `sid` int(11) NOT NULL DEFAULT '0',
  `urlabs` varchar(500) NOT NULL DEFAULT '',
  `urlsrc` varchar(500) NOT NULL DEFAULT '',
  `dateline` int(11) NOT NULL DEFAULT '0',
  `lastupdate` int(11) NOT NULL DEFAULT '0',
  `path` varchar(100) NOT NULL DEFAULT '',
  `filename` varchar(100) NOT NULL DEFAULT '',
  `size` bigint(20) NOT NULL DEFAULT '0',
  `status` int(11) NOT NULL DEFAULT '0',
  `connecterror` varchar(1000) NOT NULL DEFAULT '',
  `duration` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`imgid`),
  KEY `idx_cuid` (`cuid`),
  KEY `idx_sid` (`sid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `listurls`
-- ----------------------------
DROP TABLE IF EXISTS `listurls`;
CREATE TABLE `listurls` (
  `luid` int(11) NOT NULL AUTO_INCREMENT,
  `sid` int(11) NOT NULL DEFAULT '0',
  `listurl` varchar(500) NOT NULL DEFAULT '',
  `source` mediumtext NOT NULL,
  `dateline` int(11) NOT NULL DEFAULT '0',
  `lastupdate` int(11) NOT NULL DEFAULT '0',
  `connectcount` int(11) NOT NULL DEFAULT '0',
  `parsecount` int(11) NOT NULL DEFAULT '0',
  `isfirst` int(11) NOT NULL DEFAULT '0',
  `isenable` int(11) NOT NULL DEFAULT '0',
  `status` int(11) NOT NULL DEFAULT '0',
  `connecterror` varchar(1000) NOT NULL DEFAULT '',
  `parseerror` varchar(1000) NOT NULL DEFAULT '',
  `duration` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`luid`),
  KEY `idx` (`isenable`,`status`),
  KEY `idx_isfirst` (`isfirst`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `sites`
-- ----------------------------
DROP TABLE IF EXISTS `sites`;
CREATE TABLE `sites` (
  `sid` int(11) NOT NULL AUTO_INCREMENT,
  `sitename` varchar(50) NOT NULL DEFAULT '',
  `siteurl` varchar(500) NOT NULL DEFAULT '',
  `entryurl` varchar(500) NOT NULL DEFAULT '',
  `urlformat` varchar(1000) NOT NULL DEFAULT '',
  `isenable` int(11) NOT NULL DEFAULT '0',
  `isweixin` int(11) NOT NULL DEFAULT '0',
  `wxconfig` varchar(1000) NOT NULL DEFAULT '',
  `isbrowser` int(11) NOT NULL DEFAULT '0',
  `isscroll` int(11) NOT NULL DEFAULT '0',
  `islocal` int(11) NOT NULL DEFAULT '0',
  `listjson` varchar(1000) NOT NULL DEFAULT '',
  `listhtml` varchar(1000) NOT NULL DEFAULT '',
  `contentjson` varchar(1000) NOT NULL DEFAULT '',
  `contenthtml` varchar(1000) NOT NULL DEFAULT '',
  `interval` int(11) NOT NULL DEFAULT '20',
  `schedule` int(11) NOT NULL DEFAULT '3600',
  `lastfetch` int(11) NOT NULL DEFAULT '0',
  `parseerror` varchar(1000) NOT NULL DEFAULT '',
  `listcreated` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`sid`)
) ENGINE=MyISAM AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;

-- ----------------------------
--  Records of `sites`
-- ----------------------------
BEGIN;
INSERT INTO `sites` VALUES ('1', '爱范儿|评测', 'http://www.ifanr.com/category/review', '', '{\n \"url\": \"http://www.ifanr.com/api/v3.0/?action=latest&posts_per_page=10&page=<num>&category_name=review\",\n \"numfirst\": 1,\n \"numlast\": 5\n}', '1', '0', '', '0', '0', '0', '{\n  \"contenturl\": \"| data || link\",\n  \"urlprefix\": \"http://www.ifanr.com/\"\n}\n', '', '', '{\n  \"content\": \"article\",\n  \"title\": \"h1[class=c-article-header__title], h1[class=c-single-normal__title]\",\n  \"author\": \"span[class=o-article-header__author__name c-article-header__author__name]\",\n  \"postdate\": \"span.c-article-header-meta__time\",\n  \"tag\": \"div.c-article-tags > a\",\n  \"titleimage\": \"\"\n}', '20', '3600', '0', '', '0'), ('2', '雷科技|新闻', 'http://www.leikeji.com/columns/articles/%E6%96%B0%E9%97%BB', '', '{\n \"url\": \"http://www.leikeji.com/columns/getArticleList?ifHome=1&status=1&channels=1&pageIndex=<num>&pageSize=10&orderBy=postDate&orderType=desc&colName=%E6%96%B0%E9%97%BB\",\n \"numfirst\": 1,\n \"numlast\": 5\n}', '1', '0', '', '0', '0', '0', '{\n  \"contenturl\": \"| data || onlyUrl\",\n  \"urlprefix\": \"\",\n  \"extraprefix\": \"http://www.leikeji.com/article/<id>\"\n}', '', '', '{\n  \"content\": \"div.article-content\",\n  \"title\": \"h1.article-title\",\n  \"author\": \"div[class=article-info clr] > div.user-info > a.username\",\n  \"postdate\": \"div[class=article-info clr] > div.user-info > span.time-info\",\n  \"tag\": \"ul.tag-list > li.tag-item > a\",\n  \"titleimage\": \"div.article-header > div.photo-block > img\"\n}\n', '18', '3600', '0', '', '0'), ('3', '公众号 | 硬蛋', '', '', '', '1', '1', '{\n    \"keyword\": \"硬蛋\",\n    \"contenttype\": 0\n}', '1', '0', '1', '', '', '', '', '30', '3600', '0', '', '0'), ('4', '智东西早报', 'http://zhidx.com/p/category/daily', '', '{\n \"url\": \"http://zhidx.com/p/category/daily/page/<num>\",\n \"numfirst\": 1,\n \"numlast\": 5\n}', '1', '0', '', '0', '0', '0', '', '{\n  \"list\": \"div[class=tabCont tabCont1] > ul > li\",\n  \"contenturl\": \"p.name > a\",\n  \"urlprefix\": \"\",\n  \"attr\": \"\"\n}', '', '{\n  \"content\": \"div.article > div.finCnt\",\n  \"title\": \"div.article > div.finTit > h1\",\n  \"author\": \"\",\n  \"postdate\": \"em.time\",\n  \"tag\": \"\",\n  \"titleimage\": \"div.article > div.finPic > img\"\n}', '20', '3600', '0', '', '0'), ('5', '高新网', 'http://www.chinahightech.com/html/chaopin/index.html', 'http://www.chinahightech.com/html/chaopin/index.html', '{\n \"url\": \"http://www.chinahightech.com/html/chaopin/<num>.html\",\n \"numfirst\": 2,\n \"numlast\": 5\n}', '1', '0', '', '0', '0', '0', '', '{\n  \"list\": \"div#index-gz > ul > li\",\n  \"contenturl\": \"h3 > a\",\n  \"urlprefix\": \"\",\n  \"attr\": \"\"\n}', '', '{\n  \"content\": \"div.content\",\n  \"title\": \"div.title_nr > h1\",\n  \"author\": \"div.source\",\n  \"postdate\": \"div.addtime\",\n  \"tag\": \"\",\n  \"titleimage\": \"\"\n}', '20', '3600', '0', '', '0'), ('6', '雷锋网|精读', 'http://www.leiphone.com/category/jingdu', '', '{\n \"url\": \"http://www.leiphone.com/category/jingdu/page/<num>#lph-pageList\",\n \"numfirst\": 1,\n \"numlast\": 5\n}', '1', '0', '', '0', '0', '0', '', '{\n  \"list\": \"div[class=lph-pageList index-pageList] > div.wrap > ul > li\",\n  \"contenturl\": \"div.word > a\",\n  \"urlprefix\": \"\",\n  \"attr\": \"\"\n}', '', '{\n  \"content\": \"div[class=pageCont lph-article-comView ]\",\n  \"title\": \"div.pageTop > h1\",\n  \"author\": \"div[class=pi-author] > a\",\n  \"postdate\": \"div[class=pi-author] > span\",\n  \"tag\": \"\",\n  \"titleimage\": \"div[class=pageCont lph-article-comView ] img\"\n}', '20', '3600', '0', '', '0'), ('7', '极果|评测', 'http://www.jiguo.com/more/article.html', '', '{\n \"url\": \"http://www.jiguo.com/more/article.html?size=8&p=<num>\",\n \"numfirst\": 0,\n \"numlast\": 5\n}', '1', '0', '', '0', '0', '0', '', '{\n  \"list\": \"li.msg\",\n  \"contenturl\": \"a\",\n  \"urlprefix\": \"http://www.jiguo.com/article/index/\",\n  \"attr\": \"\"\n}', '', '{\n  \"content\": \"div.wz_con\",\n  \"title\": \"div.desc > p.title\",\n  \"author\": \"div#ajax-layout-user-card span.info-username\",\n  \"postdate\": \"div.layout-article-time > span.layout-time\",\n  \"tag\": \"\",\n  \"titleimage\": \"div.wz_con img\"\n}', '20', '3600', '0', '', '0');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
