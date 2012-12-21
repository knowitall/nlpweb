package edu.washington.cs.knowitall
package nlpweb
package tool

import scala.Array.canBuildFrom
import common.Timing
import edu.washington.cs.knowitall.tool.postag.OpenNlpPostagger
import edu.washington.cs.knowitall.tool.postag.StanfordPostagger
import edu.washington.cs.knowitall.tool.postag.PostaggedToken

class PostaggerIntent extends ToolIntent("postagger", List("opennlp", "stanford")) {
  override val info = "Enter sentences to be part-of-speech tagged, one per line."
  lazy val postaggers = Map(
    "opennlp" -> new OpenNlpPostagger(),
    "stanford" -> new StanfordPostagger())

  override def doPost(tool: String, text: String) = {
    val postagger = postaggers(tool)

    val lines = text.split("\n")
    val (postagTime, postaggeds) = Timing.time(lines.map(postagger.postag(_)))
    ("time: " + Timing.Milliseconds.format(postagTime),
      postaggeds.map {
        postagged => buildTable(List("string", "postag"), postagged.map { case PostaggedToken(postag, string, offset) => List(string, postag) })
      }.mkString("<br>\n"))
  }
}

