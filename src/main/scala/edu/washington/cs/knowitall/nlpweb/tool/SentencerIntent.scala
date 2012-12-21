package edu.washington.cs.knowitall
package nlpweb
package tool

import common.Timing
import edu.washington.cs.knowitall.tool.sentence.OpenNlpSentencer

class SentencerIntent extends ToolIntent("sentencer", List("opennlp")) {
  override val info = "Enter a single block of text (paragraph) to split into sentences."

  lazy val sentencers = Map(
    "opennlp" -> new OpenNlpSentencer())

  override def doPost(tool: String, text: String) = {
    val sentencer = sentencers(tool)

    val (sentencerTime, sentenced) = Timing.time(sentencer.sentences(text))
    ("time: " + Timing.Milliseconds.format(sentencerTime),
    sentenced.map("<li>" + _).mkString("<ol>\n", "\n", "</ol>"))
  }
}
