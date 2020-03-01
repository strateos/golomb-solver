import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.golomb"
ThisBuild / organizationName := "golomb"

lazy val root = (project in file("."))
  .settings(
    name := "golomb",
    libraryDependencies += scalaTest % Test,
    libraryDependencies ++= Seq(akkaActor, akkaHttp, akkaStream)
  )
