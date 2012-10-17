package edu.washington.cs.knowitall
package nlpweb

import common.Timing
import tool.sentence.OpenNlpSentencer

class SentencerFilter extends ToolFilter("sentencer", List("opennlp")) {
  override val info = "Enter a single block of text (paragraph) to split into sentences."

  lazy val sentencers = Map(
    "opennlp" -> new OpenNlpSentencer())

  override def doPost(params: Map[String, String]) = {
    val sentencer = sentencers(params("sentencer"))
    val text = params("text")

    val (sentencerTime, sentenced) = Timing.time(sentencer.sentences(text))
    ("time: " + Timing.Milliseconds.format(sentencerTime),
    sentenced.map("<li>" + _).mkString("<ol>\n", "\n", "</ol>"))
  }
}
