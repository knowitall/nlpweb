import sbt._
import Keys._

object NlpwebBuild extends Build {
  val nlpwebGroupId = "edu.washington.cs.knowitall.nlptools"
  val nlpwebVersion = "2.4.2-SNAPSHOT"

  lazy val root = Project(id = "nlpweb",
                          base = file("."),
                          settings = Project.defaultSettings)
}
