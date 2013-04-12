package edu.knowitall
package nlpweb
package tool

import common.Timing
import edu.knowitall.nlpweb.ToolIntent
import edu.knowitall.tool.parse.ConstituencyParser
import edu.knowitall.tool.parse.OpenNlpParser
import edu.knowitall.tool.parse.StanfordParser

object ConstituencyParserIntent
extends ToolIntent[ConstituencyParser]("constituency",
    List("stanford" -> "StanfordConstituencyParser", "opennlp" -> "OpenNlpConstituencyParser")) {
  override val info = "Enter a single sentence to be parsed."

  def constructors: PartialFunction[String, ConstituencyParser] = {
    case "OpenNlpConstituencyParser" => new OpenNlpParser()
    case "StanfordConstituencyParser" => new StanfordParser()
  }

  override def post[A](shortToolName: String, text: String, params: Map[String, String]) = {
    val parser = getTool(nameMap(shortToolName))
    var (parseTime, graph) = parser.synchronized {
      Timing.time(parser.parse(text))
    }

    val buffer = new StringBuffer()
    graph.printDOT(buffer)
    val dot = buffer.toString

    val base64Image = DotIntent.dotbase64(dot, "png")

    ("parse time: " + Timing.Milliseconds.format(parseTime),
     "<img src=\"data:image/png;base64," + base64Image + "\" />")
  }
}
