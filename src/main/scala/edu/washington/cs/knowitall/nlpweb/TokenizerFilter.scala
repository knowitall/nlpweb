package edu.washington.cs.knowitall
package nlpweb

import common._
import tool.tokenize._

import org.scalatra._
import scalate.ScalateSupport
import java.net.URL

import scala.collection.JavaConversions._

import org.apache.commons.io.IOUtils

import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter

class TokenizerFilter extends ToolFilter("tokenizer", List("stanford", "opennlp")) {
  override val info = "Enter sentences to be tokenized, one per line."
  lazy val tokenizers = Map(
    "stanford" -> new StanfordTokenizer(),
    "opennlp" -> new OpenNlpTokenizer())

  override def doPost(params: Map[String, String]) = {
    val tokenizer = tokenizers(params("tokenizer"))
    val text = params("text")

    val lines = text.split("\n")
    val (tokenizeTime, tokenized) = Timing.time(lines.map(tokenizer.tokenize(_)))
    ("time: " + Timing.Milliseconds.format(tokenizeTime),
        tokenized.map(_.mkString(" ")).mkString("\n"))
  }
}
