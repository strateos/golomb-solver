import sbt._

object Dependencies {
  lazy val scalaTest  = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val akkaActor  = "com.typesafe.akka" %% "akka-actor" % "2.5.6"
  lazy val akkaHttp   = "com.typesafe.akka" %% "akka-http"   % "10.0.11"
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.5.6"
  lazy val json4s     = "org.json4s" %% "json4s-native" % "3.6.7"
}
