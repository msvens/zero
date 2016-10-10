import sbt._
import Keys._

object Dependencies {

  val json4sVersion = "3.4.1"

  //for testing
  val junit = "junit" % "junit" % "4.11"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.4"
  val testDeps = Seq(junit,scalatest)

  val json4s = "org.json4s" %% "json4s-native" % json4sVersion
  val json4sext = "org.json4s" %% "json4s-ext" % json4sVersion

  val jsonDeps = Seq(json4s, json4sext)


}
