import AssemblyKeys._ // put this at the top of the file
assemblySettings

name := "nlpweb"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers ++= Seq("oss snapshot" at "http://oss.sonatype.org/content/repositories/snapshots/")

libraryDependencies ++= Seq(
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
    "net.databinder" %% "unfiltered-scalate" % "0.6.8",
    "net.databinder" %% "unfiltered-filter" % "0.6.8",
    "net.databinder" %% "unfiltered-jetty" % "0.6.8",
    "edu.washington.cs.knowitall.chunkedextractor" %% "chunkedextractor" % "1.0.8-SNAPSHOT",
    nlptoolsGroupId %% "nlptools-core" % nlptoolsVersion,
    nlptoolsGroupId %% "nlptools-stem-morpha" % nlptoolsVersion,
    "edu.washington.cs.knowitall.srlie" %% "srlie" % "1.0.2",
    "com.github.scopt" %% "scopt" % "2.1.0",
    "org.apache.commons" % "commons-lang3" % "3.1",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "ch.qos.logback" % "logback-core" % "1.0.13",
    "org.slf4j" % "slf4j-api" % "1.7.5",
    "commons-io" % "commons-io" % "2.4",
    "org.orbroker" %% "orbroker" % "3.2.1-1",
    "org.apache.derby" % "derby" % "10.10.1.1",
    "org.riedelcastro" % "whatswrong" % "0.2.4")

licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

mainClass in assembly := Some("edu.knowitall.nlpweb.NlpWeb")

mergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) => MergeStrategy.discard
      case _ => MergeStrategy.discard
    }
  case _ => MergeStrategy.first
}
