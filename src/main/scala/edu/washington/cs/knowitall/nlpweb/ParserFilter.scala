package edu.washington.cs.knowitall
package nlpweb

import common._
import org.scalatra._
import java.net.URL
import scalate.ScalateSupport
import scala.collection.JavaConversions._
import org.apache.commons.io.IOUtils
import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter
import edu.washington.cs.knowitall.tool.parse._
import edu.washington.cs.knowitall.tool.parse.pattern._
import edu.washington.cs.knowitall.tool.parse.graph._
import edu.washington.cs.knowitall.tool.parse.BaseStanfordParser._

class ParserFilter extends ToolFilter("parser", List("stanford", "deserialize")) {
  override val info = "Enter a single sentence to be parsed."

  lazy val stanfordParser = new StanfordParser()
//  lazy val maltParser = new MaltParser()
  lazy val deserializeParser = new DependencyParser {
    override def dependencyGraph(pickled: String) = 
      DependencyGraph.deserialize(pickled)
      
    override def dependencies(pickled: String) =
      DependencyGraph.deserialize(pickled).dependencies
  }

  val parsers = tools
  def getParser(parser: String): DependencyParser =
    parser match {
      case "stanford" => stanfordParser
//      case "malt" => maltParser
      case "deserialize" => deserializeParser
    }

  override def config(params: Map[String, String]): String =
    config(
      params.get("pattern"),
      params.keys.contains("ccCompressed"),
      params.keys.contains("collapseNounGroups"),
      params.keys.contains("collapsePrepOf"),
      params.keys.contains("collapseWeakLeaves"))

  def config(pattern: Option[String], ccCompressed: Boolean, collapseNounGroups: Boolean, collapsePrepOf: Boolean, collapseWeakLeaves: Boolean): String = """
    pattern: <input name="pattern" type="input" size="60" value="""" + pattern.getOrElse("") + """" /><br />
    <input name="ccCompressed" type="checkbox" value="true" """ + (if (true) """checked="true" """ else "") + """ /> CC Compressed<br />
    <input name="collapseNounGroups" type="checkbox" value="true" """ + (if (true) """checked="true" """ else "") + """/> Collapse Noun Groups<br />
    <input name="collapsePrepOf" type="checkbox" value="true" """ + (if (true) """checked="true" """ else "") + """/> Collapse Prep Of<br />
    <input name="collapseWeakLeaves" type="checkbox" value="true" """ + (if (true) """checked="true" """ else "") + """/> Collapse Weak Leaves<br />
    <br />"""

  override def doPost(params: Map[String, String]) = {
    val parser = getParser(params("parser"))
    val input = params("text")
    val pattern = params("pattern")
    var (parseTime, graph) = parser.synchronized {
      Timing.time(
        parser match {
          case parser: BaseStanfordParser =>
            // if it's a StanfordBaseParser consider doing ccCompressed
            parser.dependencyGraph(input, if (params.getOrElse("ccCompressed", "") == "true") CCCompressed else None)
          case parser: DependencyParser =>
            parser.dependencyGraph(input)
        })
    }
    
    if (params.getOrElse("collapseNounGroups", "") == "true") {
      graph = graph.collapseNounGroups()
    }

    if (params.getOrElse("collapsePrepOf", "") == "true") {
      graph = graph.collapseNNPOf
    }

    if (params.getOrElse("collapseWeakLeaves", "") == "true") {
      graph = graph.collapseWeakLeaves
    }

    val (nodes, edges) = if (!params.get("pattern").map(_.isEmpty).getOrElse(false)) {
      val pattern = DependencyPattern.deserialize(params("pattern").trim)
      val matches = pattern(graph.graph)
      (for (m <- matches; v <- m.bipath.nodes) yield v,
        for (m <- matches; e <- m.bipath.edges) yield e)
    }
    else (List(), List())

    val buffer = new StringBuffer()
    graph.printDotWithHighlights(buffer, if (input.length > 100) input.substring(0, 100) + "..." else input, nodes.toSet, edges.toSet)
    val rawDot = buffer.toString
    val dot = rawDot
      .replaceAll("\n", " ")
      .replaceAll("""\?|#|%|^|~|`|@|&|\$""", "")
      .replaceAll("""\s+""", " ")
      .replaceAll("\"", """%22""")
      .replaceAll(" ", "%20")

    ("parse time: " + Timing.Milliseconds.format(parseTime),
      "<img src=\"" + servletContext.getContextPath + "/dot/png/" + dot + "\" /><br><pre>serialized: " + graph.serialize + "\n\n" + rawDot + "</pre>")
  }
}
