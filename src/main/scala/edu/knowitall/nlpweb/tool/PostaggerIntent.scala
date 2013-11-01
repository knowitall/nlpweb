package edu.knowitall
package nlpweb
package tool

import java.net.URL

import scala.Array.canBuildFrom

import org.apache.commons.lang.NotImplementedException

import common.Timing
import edu.knowitall.nlpweb.ToolIntent
import edu.knowitall.nlpweb.visualize.Whatswrong.CanWrite
import edu.knowitall.tool.postag.PostaggedToken
import edu.knowitall.tool.postag.Postagger
import edu.knowitall.tool.postag.RemotePostagger
import visualize.Whatswrong.Base64String
import visualize.Whatswrong.CanWrite
import visualize.Whatswrong.writeGraphic2Base64

object PostaggerIntent
extends ToolIntent[Postagger]("postag",
    List("opennlp" -> "OpenNlpPostagger", "stanford" -> "StanfordPostagger")) {
  override val info = "Enter sentences to be part-of-speech tagged, one per line."

  def constructors = PartialFunction.empty[String, Postagger] /* = {
    case "OpenNlpPostagger" => new OpenNlpPostagger()
    case "StanfordPostagger" => new StanfordPostagger()
  }
  */
  override def remote(url: java.net.URL) = new RemotePostagger(url.toString)

  def image(tokens: Seq[PostaggedToken]) = {
    import visualize.Whatswrong._
    val b64 = implicitly[CanWrite[Seq[PostaggedToken], Base64String]].write(tokens)
    "<img src=\"data:image/png;base64," + b64.string + "\">"
  }

  override def post[A](shortToolName: String, text: String, params: Map[String, String]) = {
    val postagger = getTool(nameMap(shortToolName))

    val lines = text.split("\n")
    val (postagTime, postaggeds) = Timing.time(lines.map(postagger.postag(_)))
    ("time: " + Timing.Milliseconds.format(postagTime),
      postaggeds.map {
        postagged => buildTable(List("string", "postag"), postagged.map { case PostaggedToken(postag, string, offset) => List(string, postag) })
      }.mkString("<br>\n") + "<br />" + (postaggeds map image).mkString("<p>", "\n", "</p>"))
  }
}

