val repoName = "zero"
val homePage = "http://www.mellowtech.org/"

organizationName := "Mellowtech"
organizationHomepage := Some(url(homePage))

scmInfo := Some(
  ScmInfo(
    url(s"https://github.com/msvens/${repoName}.git"),
    s"scm:git:git@github.com:msvens/${repoName}.git",
    s"scm:git:git@github.com:msvens/${repoName}.git"
  )
)

developers := List(
  Developer(
    id    = "msvens",
    name  = "Martin Svensson",
    email = "msvens@gmail.com",
    url   = url(homePage)
  )
)

description := "Simple Json Http Client"
licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url(s"https://github.com/msvens/${repoName}"))

pomIncludeRepository := { _ => false }
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle := true

// Your profile name of the sonatype account. The default is the same with the organization value
//sonatypeProfileName := "org.mellowtech"
