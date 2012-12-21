package edu.washington.cs.knowitall
package nlpweb

import scala.Array.canBuildFrom

import common.Timing
import tool.postag.{OpenNlpPostagger, PostaggedToken, StanfordPostagger}

class PostaggerFilter extends ToolFilter("postagger", List("opennlp", "stanford")) {
  override val info = "Enter sentences to be part-of-speech tagged, one per line."
  lazy val postaggers = Map(
    "opennlp" -> new OpenNlpPostagger(),
    "stanford" -> new StanfordPostagger())

  override def doPost(params: Map[String, String]) = {
    val postagger = postaggers(params("postagger"))
    val text = params("text")

    val lines = text.split("\n")
    val (postagTime, postaggeds) = Timing.time(lines.map(postagger.postag(_)))
    ("time: " + Timing.Milliseconds.format(postagTime),
      postaggeds.map { 
        postagged => buildTable(List("string", "postag"), postagged.map { case PostaggedToken(postag, string, offset) => List(string, postag) })
      }.mkString("<br>\n"))
  }
}

