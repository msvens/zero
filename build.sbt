import Dependencies._
import sbt.Keys._
import sbt._

ThisBuild / version := "0.3-SNAPSHOT"
ThisBuild / organization := "org.mellowtech"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / Test / publishArtifact := false
ThisBuild / Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/site/test-reports")

lazy val server = (project in file ("server")).
  enablePlugins(JavaServerAppPackaging,DebianPlugin,SystemVPlugin).
  settings(
    name := "zero-server",
    libraryDependencies ++= serverDependcies
  ).
  settings(
    mainClass in Compile := Some("org.mellowtech.zero.server.ZeroGrpcServer"),
    maintainer in Linux := "Martin Svensson <msvens@gmail.com>",
    packageSummary in Linux := "Timer Service",
    packageDescription in Linux := "This package installs a akka-http timer service that can be quereied using a grpc api",
    daemonUser in Linux := "www-data"
    //serverLoading in Debian := ServerLoader.SystemV
  ).dependsOn(commons)

lazy val client = (project in file ("client")).
  settings(
    name := "zero-client",
    libraryDependencies ++= clientDependcies,
  ).dependsOn(commons)

lazy val commons = (project in file ("commons")).
  enablePlugins(AkkaGrpcPlugin).
  settings(
    name := "zero-commons",
    libraryDependencies ++= commonsDependcies,
  )


lazy val root = (project in file (".")).aggregate(commons,server,client).
  settings(
    publish / skip := true
  )

