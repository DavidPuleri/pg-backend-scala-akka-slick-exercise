name := "scala"

version := "0.1"

scalaVersion := "2.12.8"

val opRabbitVersion = "2.1.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",
  "org.json4s" %% "json4s-native" % "3.6.5",
  "de.heikoseeberger" %% "akka-http-json4s" % "1.25.2",
  "com.typesafe.slick" %% "slick" % "3.3.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.0",
  "mysql" % "mysql-connector-java" % "8.0.16",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.spingo" %% "op-rabbit-core"        % opRabbitVersion,
  "com.spingo" %% "op-rabbit-play-json"   % opRabbitVersion,
  "com.spingo" %% "op-rabbit-json4s"      % opRabbitVersion,
  "com.spingo" %% "op-rabbit-airbrake"    % opRabbitVersion,
  "com.spingo" %% "op-rabbit-akka-stream" % opRabbitVersion
  
)