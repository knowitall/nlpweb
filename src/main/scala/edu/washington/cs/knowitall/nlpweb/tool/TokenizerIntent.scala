package edu.washington.cs.knowitall
package nlpweb
package tool

import scala.Array.canBuildFrom
import common.Timing
import edu.washington.cs.knowitall.tool.tokenize.StanfordTokenizer
import edu.washington.cs.knowitall.tool.tokenize.OpenNlpTokenizer

class TokenizerIntent extends ToolIntent("tokenizer", List("stanford", "opennlp")) {
  override val info = "Enter sentences to be tokenized, one per line."
  lazy val tokenizers = Map(
    "stanford" -> new StanfordTokenizer(),
    "opennlp" -> new OpenNlpTokenizer())

  override def post(tool: String, text: String) = {
    val tokenizer = tokenizers(tool)

    val lines = text.split("\n")
    val (tokenizeTime, tokenized) = Timing.time(lines.map(tokenizer.tokenize(_)))
    ("time: " + Timing.Milliseconds.format(tokenizeTime),
        tokenized.map(_.mkString(" ")).mkString("\n"))
  }
}
