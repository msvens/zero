// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "org.mellowtech"

//useGpg := true
//pgpPassphrase := Some(Array('1','9','K','a','o','s','7','3','.'))

// To sync with Maven central, you need to supply the following information:
pomExtra in Global := {
  <url>https://github.com/msvens/zero</url>
    <scm>
      <developerConnection>scm:git:git@github.com:msvens/zero.git</developerConnection>
      <connection>scm:git:git@github.com:msvens/zero.git</connection>
      <url>git@github.com:msvens/zero.git</url>
    </scm>
    <licenses>
      <license>
        <name>The Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <developers>
      <developer>
        <name>Martin Svensson</name>
        <email>msvens@gmail.com</email>
        <organization>Mellowtech</organization>
        <organizationUrl>http://www.mellowtech.org/</organizationUrl>
      </developer>
    </developers>
}
