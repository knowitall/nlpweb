package edu.washington.cs.knowitall
package nlpweb

import common.Timing
import edu.washington.cs.knowitall.tool.parse.{ConstituencyParser, OpenNlpParser, StanfordParser}

class ConstituencyParserFilter extends ToolFilter("constituency", List("stanford", "opennlp")) {
  override val info = "Enter a single sentence to be parsed."
  lazy val stanfordParser = new StanfordParser()
  lazy val openNlpParser = new OpenNlpParser()

  val parsers = tools
  def getParser(parser: String): ConstituencyParser =
    parser match {
      case "stanford" => stanfordParser
      case "opennlp" => openNlpParser
    }

  override def doPost(params: Map[String, String]) = {
    val parser = getParser(params("constituency"))
    val input = params("text")
    var (parseTime, graph) = parser.synchronized {
      Timing.time(parser.parse(input))
    }

    val buffer = new StringBuffer()
    graph.printDOT(buffer)
    val dot = buffer.toString
      .replaceAll("""\\n""", "")
      .replaceAll("""\s+""", " ")
      .replaceAll("\"", """%22""")

    ("parse time: " + Timing.Milliseconds.format(parseTime),
     "<img src=\"" + servletContext.getContextPath + "/dot/png/" + dot + "\" />")
  }
}
