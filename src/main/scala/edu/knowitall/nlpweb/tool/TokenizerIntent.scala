package edu.knowitall
package nlpweb
package tool

import scala.Array.canBuildFrom
import common.Timing
import edu.knowitall.tool.tokenize.StanfordTokenizer
import edu.knowitall.tool.tokenize.OpenNlpTokenizer
import unfiltered.request.HttpRequest
import edu.knowitall.tool.tokenize.Token
import java.awt.image.BufferedImage
import edu.knowitall.tool.tokenize.Tokenizer
import edu.knowitall.tool.tokenize.RemoteTokenizer

object TokenizerIntent
extends ToolIntent[Tokenizer]("tokenize",
    List("stanford" -> "StanfordTokenizer", "opennlp" -> "OpenNlpTokenizer")) {
  override val info = "Enter sentences to be tokenized, one per line."

  def constructors: PartialFunction[String, Tokenizer] = {
    case "StanfordTokenizer" => new StanfordTokenizer()
    case "OpenNlpTokenizer" => new OpenNlpTokenizer()
  }
  override def remote(url: java.net.URL) = new RemoteTokenizer(url.toString)

  def image(tokens: Seq[Token]) = {
    import visualize.Whatswrong._
    val b64 = implicitly[CanWrite[Seq[Token], Base64String]].write(tokens)
    "<img src=\"data:image/png;base64," + b64.string + "\">"
  }

  override def post[A](shortToolName: String, text: String, params: Map[String, String]) = {
    val tokenizer = getTool(nameMap(shortToolName))

    val lines = text.split("\n")
    val (tokenizeTime, tokenized) = Timing.time(lines.map(tokenizer.tokenize(_)))
    ("time: " + Timing.Milliseconds.format(tokenizeTime),
        tokenized.map(tokens => "<p>" + tokens.mkString(" ") + "</p>").mkString("\n") + "<br>" + tokenized.map(tokens => "<p>" + image(tokens) + "</p>").mkString("\n"))
  }
}
