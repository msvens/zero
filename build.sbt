import Dependencies._
import sbt.Keys._
import sbt._

lazy val buildSettings = Seq(
  version := "0.2-SNAPSHOT",
  organization := "org.mellowtech",
  scalaVersion := "2.13.0",
  publishArtifact in Test := false,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/site/test-reports")
)

lazy val server = (project in file ("server")).
  enablePlugins(JavaServerAppPackaging,DebianPlugin,SystemVPlugin).
  settings(buildSettings: _*).
  settings(
    name := "zero-server",
    libraryDependencies ++= serverDependcies,
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
    //javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test",
    mainClass in Compile := Some("org.mellowtech.zero.server.ZeroGrpcServer"),
    maintainer in Linux := "Martin Svensson <msvens@gmail.com>",
    packageSummary in Linux := "Timer Service",
    packageDescription in Linux := "This package installs a akka-http timer service that can be queired using a grpc api",
    daemonUser in Linux := "www-data"
    //serverLoading in Debian := ServerLoader.SystemV
  ).dependsOn(commons)

lazy val client = (project in file ("client")).
  settings(buildSettings: _*).
  settings(
    name := "zero-client",
    libraryDependencies ++= clientDependcies,
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  ).dependsOn(commons)

lazy val commons = (project in file ("commons")).
  enablePlugins(AkkaGrpcPlugin).
  settings(buildSettings: _*).
  settings(
    name := "zero-commons",
    libraryDependencies ++= commonsDependcies,
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

