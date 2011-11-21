package edu.washington.cs.knowitall
package nlpweb

import common._
import tool.tokenize._

import org.scalatra._
import scalate.ScalateSupport
import java.net.URL

import collection.JavaConversions._

import org.apache.commons.io.IOUtils

import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter

import edu.washington.cs.knowitall.nlpweb.Common._

class TokenizerFilter extends ToolFilter("tokenizer", List("stanford", "opennlp")) {
  override val info = "Enter sentences to be tokenized, one per line."
  lazy val tokenizers = Map(
    "stanford" -> new StanfordTokenizer(),
    "opennlp" -> new OpenNlpTokenizer())

  override def doPost(params: Map[String, String]) = {
    val tokenizer = tokenizers(params("tokenizer"))
    val text = params("text")

    val lines = text.split("\n")
    val (tokenizeTime, tokenized) = timed(lines.map(tokenizer.tokenize(_)))
    ("time: " + Timing.format(tokenizeTime),
        tokenized.map(_.mkString(" ")).mkString("\n"))
  }
}
