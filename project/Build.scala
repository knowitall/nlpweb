import sbt._
import Keys._

object NlpwebBuild extends Build {
  val nlpwebGroupId = "edu.washington.cs.knowitall.nlptools"
  val nlpwebVersion = "2.4.1"

  lazy val root = Project(id = "nlpweb",
                          base = file("."),
                          settings = Project.defaultSettings)
}
