name := "inheater-collector"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.5.3"
  val akkaHttpV = "10.0.9"
  Seq(
    "com.typesafe.akka" %% "akka-actor"  % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % "10.0.9",
    "com.typesafe.play" %% "play-json" % "2.6.1",
    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.0",
		"org.scala-lang.modules" %% "scala-async" % "0.9.6",
	  "com.typesafe.slick" %% "slick" % "3.2.0",
  	"org.seleniumhq.selenium" % "selenium-java" % "2.53.0",
	  "org.jsoup" % "jsoup" % "1.10.3",
    "org.slf4j" % "slf4j-simple" % "1.7.10",
    "mysql" % "mysql-connector-java" % "6.0.6"
  )
}

