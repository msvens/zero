addSbtPlugin("io.crashbox" % "sbt-gpg" % "0.2.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.5")
//enablePlugins(JavaAppPackaging)
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.11")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.24")

//grpc
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "0.7.1")
addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.4")
