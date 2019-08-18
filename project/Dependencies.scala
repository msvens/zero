import sbt._
import Keys._



object Dependencies {

  //resolvers += Resolver.bintrayRepo("akka", "maven")

  val logbackClassicVersion = "1.2.3"
  val scallopVersion = "3.3.1"
  val scoptVersion = "3.7.1"

  //versions
  val hikariCpVersion = "3.3.1"
  val slickVersion = "3.3.2"
  val postgresqlVersion = "42.2.6"

  val akkaHttpVersion = "10.1.9"
  val akkaVersion = "2.5.24"

  val junitVersion = "4.12"
  val scalatestVersion = "3.0.8"

  //test deps
  val junit = "junit" % "junit" % junitVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion
  val testDeps = Seq(junit,scalatest)

  //akka deps
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaDeps = Seq(akkaHttp, akkaStream, akkaSlf4j)

  //db deps:
  //val hikariCp = "com.zaxxer" % "HikariCP" % hikariCpVersion
  val slick = "com.typesafe.slick" %% "slick" % slickVersion
  val slickCodegen = "com.typesafe.slick" %% "slick-codegen" % slickVersion
  val slickHikariCp = "com.typesafe.slick" %% "slick-hikaricp" % slickVersion
  val postgresql = "org.postgresql" % "postgresql" % postgresqlVersion
  val dbDeps = Seq(postgresql, slick, slickCodegen, slickHikariCp)

  //protobuf
  //val scalapbRuntime = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

  //misc
  val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackClassicVersion
  val scopt = "com.github.scopt" %% "scopt" % scoptVersion
  val scallop = "org.rogach" %% "scallop" % scallopVersion

  //module dependencies:
  val serverDependcies = akkaDeps ++ testDeps ++ dbDeps ++ Seq(logbackClassic)
  val clientDependcies = akkaDeps ++ testDeps ++ Seq(scopt, scallop, logbackClassic)
  val commonsDependcies = testDeps

}
