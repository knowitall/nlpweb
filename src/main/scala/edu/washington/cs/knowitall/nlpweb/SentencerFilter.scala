package edu.washington.cs.knowitall
package nlpweb

import common._
import tool.sentence._

import org.scalatra._
import scalate.ScalateSupport
import java.net.URL

import scala.collection.JavaConversions._

import org.apache.commons.io.IOUtils

import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter

class SentencerFilter extends ToolFilter("sentencer", List("opennlp", "piao")) {
  override val info = "Enter a single block of text (paragraph) to split into sentences."

  lazy val sentencers = Map(
    "opennlp" -> new OpenNlpSentencer(),
    "piao" -> new PiaoSentencer())

  override def doPost(params: Map[String, String]) = {
    val sentencer = sentencers(params("sentencer"))
    val text = params("text")

    val (sentencerTime, sentenced) = Timing.time(sentencer.sentences(text))
    ("time: " + Timing.Milliseconds.format(sentencerTime),
    sentenced.map("<li>" + _).mkString("<ol>\n", "\n", "</ol>"))
  }
}
