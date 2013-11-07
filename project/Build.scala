import sbt._
import Keys._

object NlpwebBuild extends Build {
  val nlptoolsGroupId = "edu.washington.cs.knowitall.nlptools"
  val nlptoolsVersion = "2.4.5-SNAPSHOT"

  lazy val root = Project(id = "nlpweb",
                          base = file("."),
                          settings = Project.defaultSettings)
}
