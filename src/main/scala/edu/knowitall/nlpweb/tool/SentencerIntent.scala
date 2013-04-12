package edu.knowitall
package nlpweb
package tool

import common.Timing
import edu.knowitall.nlpweb.ToolIntent
import edu.knowitall.tool.segment.Segmenter
import edu.knowitall.tool.sentence.OpenNlpSentencer

object SentencerIntent
extends ToolIntent[Segmenter]("sentencer", List("opennlp" -> "OpenNlpSentencer")) {
  override val info = "Enter a single block of text (paragraph) to split into sentences."

  def constructors: PartialFunction[String, Segmenter] = {
    case "OpenNlpSentencer" => new OpenNlpSentencer()
  }

  override def post[A](shortToolName: String, text: String, params: Map[String, String]) = {
    val sentencer = getTool(nameMap(shortToolName))

    val (sentencerTime, sentenced) = Timing.time(sentencer.segmentTexts(text))
    ("time: " + Timing.Milliseconds.format(sentencerTime),
    sentenced.map("<li>" + _).mkString("<ol>\n", "\n", "</ol>"))
  }
}
