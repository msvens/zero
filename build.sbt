import Dependencies._
import sbt.Keys._
import sbt._

lazy val buildSettings = Seq(
  version := "0.1-SNAPSHOT",
  organization := "org.mellowtech",
  scalaVersion := "2.11.8",
  publishArtifact in Test := false,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/site/test-reports")
)

lazy val server = (project in file ("server")).
  enablePlugins(JavaServerAppPackaging,DebianPlugin,SystemVPlugin).
  settings(buildSettings: _*).
  settings(
    name := "zero-server",
    libraryDependencies ++= jsonDeps ++ testDeps ++ Seq(
      "com.typesafe.slick" %% "slick" % "3.1.1",
      "org.postgresql" % "postgresql" % "9.4-1206-jdbc41",
      "com.zaxxer" % "HikariCP" % "2.4.7",
      "de.heikoseeberger" %% "akka-http-json4s" % "1.10.1",
      "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.11",
      "ch.qos.logback" % "logback-classic" % "1.1.7"
    ),
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  ).
  settings(
    mainClass in Compile := Some("org.mellowtech.zero.server.Server"),
    maintainer in Linux := "Martin Svensson <msvens@gmail.com>",
    packageSummary in Linux := "Timer Service",
    packageDescription in Linux := "This package installs a akka-http timer service that can be queired using a json api",
    daemonUser in Linux := "www-data"
    //serverLoading in Debian := ServerLoader.SystemV
  ).dependsOn(commons)

lazy val client = (project in file ("client")).
  settings(buildSettings: _*).
  settings(
    name := "zero-client",
    libraryDependencies ++= jsonDeps ++ testDeps ++ Seq(
      "com.github.scopt" %% "scopt" % "3.5.0"
    ),
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    //assemblyJarName in assembly := "timer.jar",
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  ).dependsOn(commons)

lazy val commons = (project in file ("commons")).
  settings(buildSettings: _*).
  settings(
    name := "zero-commons",
    libraryDependencies ++= testDeps ++ jsonDeps ++ Seq(
      "org.mellowtech" %% "jsonclient" % "0.1-SNAPSHOT"
    ),
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  )


lazy val root = (project in file (".")).aggregate(commons,server,client).
  settings(buildSettings: _*).
  settings(
    publish := false
  )

