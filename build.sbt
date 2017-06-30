name := "inheater-collector"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "ansj repository" at "http://maven.nlpcn.org/"

libraryDependencies ++= {
  val akkaV = "2.4.3"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"  % akkaV,
    "com.typesafe.akka" %% "akka-remote" % akkaV,
    "com.typesafe.akka" %% "akka-cluster" % akkaV,
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-xml-experimental" % akkaV,
    "com.typesafe.play" %% "play-json" % "2.5.2",
    "org.ansj" % "ansj_seg" % "3.7.6",
    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1",
		"org.scala-lang.modules" %% "scala-async" % "0.9.5",
	  "com.typesafe.slick" %% "slick" % "3.1.1",
  	"org.seleniumhq.selenium" % "selenium-java" % "2.53.0",
	  "org.jsoup" % "jsoup" % "1.9.1",
	  "org.elasticsearch" % "elasticsearch" % "2.3.1",
    "org.slf4j" % "slf4j-simple" % "1.7.10",
  	"mysql" % "mysql-connector-java" % "5.1.38"
  )
}

//解决Elasticsearch中的包冲突
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
  case PathList("org", "joda", "time", "base", "BaseDateTime.class") => MergeStrategy.first
//  case PathList(ps @ _*) if ps.last endsWith "axiom.xml" => MergeStrategy.filterDistinctLines
//  case PathList(ps @ _*) if ps.last endsWith "Log$Logger.class" => MergeStrategy.first
//  case PathList(ps @ _*) if ps.last endsWith "ILoggerFactory.class" => MergeStrategy.first
  case x => old(x)
}}

