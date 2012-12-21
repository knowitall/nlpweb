package edu.washington.cs.knowitall
package nlpweb

import scala.collection.JavaConversions.asJavaCollection

import common.Timing
import edu.washington.cs.knowitall.tool.parse.{DependencyParser, MaltParser, StanfordParser}
import edu.washington.cs.knowitall.tool.parse.graph.{DependencyGraph, DependencyPattern}
import edu.washington.cs.knowitall.tool.stem.MorphaStemmer.instance

class ParserFilter extends ToolFilter("parser", List("malt", "stanford", "deserialize")) {
  import edu.washington.cs.knowitall.tool.stem.MorphaStemmer.instance
  override val info = "Enter a single sentence to be parsed."

  lazy val stanfordParser = new StanfordParser()
  lazy val maltParser = new MaltParser()
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
      case "malt" => maltParser
      case "deserialize" => deserializeParser
    }

  override def config(params: Map[String, String]): String =
    config(
      params.get("pattern"),
      params.keys.contains("collapsed"),
      params.keys.contains("collapseNounGroups"),
      params.keys.contains("collapsePrepOf"),
      params.keys.contains("collapseWeakLeaves"))

  def config(pattern: Option[String], collapsed: Boolean, collapseNounGroups: Boolean, collapsePrepOf: Boolean, collapseWeakLeaves: Boolean): String = """
    pattern: <input name="pattern" type="input" size="60" value="""" + pattern.getOrElse("") + """" /><br />
    <input name="collapsed" type="checkbox" value="true" """ + (if (true) """checked="true" """ else "") + """ /> Collapsed<br />
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
          case parser: DependencyParser =>
            val graph = parser.dependencyGraph(input)
            if (params.getOrElse("collapsed", "") == "true")
              graph.collapse
            else
              graph
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

    val rawDot = graph.dotWithHighlights(if (input.length > 100) input.substring(0, 100) + "..." else input, Set.empty, Set.empty)
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
