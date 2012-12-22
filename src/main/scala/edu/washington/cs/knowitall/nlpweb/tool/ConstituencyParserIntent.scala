package edu.washington.cs.knowitall
package nlpweb
package tool

import common.Timing
import edu.washington.cs.knowitall.tool.parse.{ConstituencyParser, OpenNlpParser, StanfordParser}
import unfiltered.request.HttpRequest

class ConstituencyParserIntent extends ToolIntent("constituency", List("stanford", "opennlp")) {
  override val info = "Enter a single sentence to be parsed."
  lazy val stanfordParser = new StanfordParser()
  lazy val openNlpParser = new OpenNlpParser()

  val parsers = tools
  def getParser(parser: String): ConstituencyParser =
    parser match {
      case "stanford" => stanfordParser
      case "opennlp" => openNlpParser
    }

  override def post[A](tool: String, text: String, params: Map[String, String]) = {
    val parser = getParser(tool)
    var (parseTime, graph) = parser.synchronized {
      Timing.time(parser.parse(text))
    }

    val buffer = new StringBuffer()
    graph.printDOT(buffer)
    val dot = buffer.toString
      .replaceAll("""\\n""", "")
      .replaceAll("""\s+""", " ")
      .replaceAll("\"", """%22""")

    ("parse time: " + Timing.Milliseconds.format(parseTime),
     "<img src=\"/dot/png/" + dot + "\" />")
  }
}
