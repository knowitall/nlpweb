package edu.washington.cs.knowitall
package nlpweb

import common._

import org.scalatra._
import java.net.URL
import scalate.ScalateSupport

import collection.JavaConversions._

import org.apache.commons.io.IOUtils

import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter

import edu.washington.cs.knowitall.nlpweb.Common._

import edu.washington.cs.knowitall.tool.parse._
import edu.washington.cs.knowitall.tool.parse.graph._

class ParserFilter extends ToolFilter("parser", List("stanford", "malt", "deserialize")) {
  override val info = "Enter a single sentence to be parsed."

  lazy val stanfordParser = new StanfordParser()
  lazy val maltParser = new MaltParser("engmalt.poly.mco")
  lazy val deserializeParser = new DeserializeDependencyParser()

  val parsers = tools
  def getParser(parser: String): DependencyParser =
    parser match {
      case "stanford" => stanfordParser
      case "malt" => maltParser
      case "deserialize" => deserializeParser
    }

  override def config(params: Map[String, String]): String =
    config(
      params.keys.contains("ccCompressed"),
      params.keys.contains("collapseNN"),
      params.keys.contains("collapseNounsHeuristic"),
      params.keys.contains("collapsePrepOf"))

  def config(ccCompressed: Boolean, collapseNN: Boolean, collapseNounsHeuristic: Boolean, collapsePrepOf: Boolean): String = """
    <input name="ccCompressed" type="checkbox" value="true" """ + (if (ccCompressed) """checked="true" """ else "") + """ /> CC Compressed<br />
    <input name="collapseNN" type="checkbox" value="true" """ + (if (collapseNN) """checked="true" """ else "") + """/> Collapse NN<br />
    <input name="collapseNounsHeuristic" type="checkbox" value="true" """ + (if (collapseNounsHeuristic) """checked="true" """ else "") + """/> Collapse Nouns by Heuristic<br />
    <input name="collapsePrepOf" type="checkbox" value="true" """ + (if (collapsePrepOf) """checked="true" """ else "") + """/> Collapse Prep Of<br />
    <br />"""

  override def doPost(params: Map[String, String]) = {
    val parser = getParser(params("parser"))
    val input = params("text")
    var (parseTime, graph) = parser.synchronized {
      timed(new DependencyGraph(input, 
        parser match {
          case parser: BaseStanfordParser =>
            // if it's a StanfordBaseParser consider doing ccCompressed
            parser.dependencies(input, params.getOrElse("ccCompressed", "") == "true")
          case parser: DependencyParser =>
            parser.dependencies(input)
        }))
    }

    if (params.getOrElse("collapseNN", "") == "true") {
      graph = graph.collapseNN
    }

    if (params.getOrElse("collapseNounsHeuristic", "") == "true") {
      graph = graph.collapseNounGroups
    }

    if (params.getOrElse("collapsePrepOf", "") == "true") {
      graph = graph.collapseNNPOf
    }

    val buffer = new StringBuffer()
    graph.printDOT(buffer, if (input.length > 100) input.substring(0, 100) + "..." else input)
    val rawDot = buffer.toString
    val dot = rawDot
      .replaceAll("\n", " ")
      .replaceAll("""\s+""", " ")
      .replaceAll("\"", """%22""")
      .replaceAll(" ", "%20")

    ("parse time: " + Timing.format(parseTime),
      "<img src=\"" + servletContext.getContextPath + "/dot/png/" + dot + "\" /><br><pre>dependencies: " + Dependencies.serialize(graph.dependencies) + "\n\n" + rawDot + "</pre>")
  }
}
