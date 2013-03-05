import AssemblyKeys._ // put this at the top of the file
assemblySettings

name := "nlpweb"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.9.2"

resolvers ++= Seq("oss snapshot" at "http://oss.sonatype.org/content/repositories/snapshots/")

libraryDependencies ++= Seq(
    "net.databinder" %% "unfiltered-scalate" % "0.6.3-2",
    "net.databinder" %% "unfiltered-filter" % "0.6.3",
    "net.databinder" %% "unfiltered-jetty" % "0.6.3",
    "edu.washington.cs.knowitall.chunkedextractor" %% "chunkedextractor" % "1.0.0" exclude("net.sf.jwordnet", "jwnl"),
    "edu.washington.cs.knowitall.ollie" %% "ollie-core" % "1.0.1",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-core" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-stem-morpha" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-stem-snowball" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-parse-stanford" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-parse-malt" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-parse-opennlp" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-tokenize-stanford" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-tokenize-opennlp" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-postag-opennlp" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-postag-stanford" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-chunk-opennlp" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-sentence-opennlp" % "2.3.1-SNAPSHOT",
    "edu.washington.cs.knowitall.openiesrl" %% "openie-srl" % "1.0-SNAPSHOT",
    "com.github.scopt" %% "scopt" % "2.1.0",
    "org.apache.commons" % "commons-lang3" % "3.1",
    "ch.qos.logback" % "logback-classic" % "1.0.3",
    "ch.qos.logback" % "logback-core" % "1.0.3",
    "org.slf4j" % "slf4j-api" % "1.7.1",
    "commons-io" % "commons-io" % "2.3",
    "org.orbroker" % "orbroker_2.9.1" % "3.2.1-1",
    "org.apache.derby" % "derby" % "10.9.1.0",
    "org.riedelcastro" % "whatswrong" % "0.2.4")

mainClass in assembly := Some("edu.washington.cs.knowitall.nlpweb.NlpWeb")

mergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) => MergeStrategy.discard
      case _ => MergeStrategy.discard
    }
  case _ => MergeStrategy.first
}
