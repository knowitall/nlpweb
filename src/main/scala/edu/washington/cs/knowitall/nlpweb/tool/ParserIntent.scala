package edu.washington.cs.knowitall
package nlpweb
package tool

import scala.collection.JavaConversions.asJavaCollection
import common.Timing
import edu.washington.cs.knowitall.tool.parse.{DependencyParser, MaltParser, ClearParser}
import edu.washington.cs.knowitall.tool.parse.graph.{DependencyGraph, DependencyPattern}
import edu.washington.cs.knowitall.tool.stem.MorphaStemmer
import unfiltered.request.HttpRequest
import org.apache.commons.codec.net.URLCodec

object ParserIntent extends ToolIntent("parser", List("malt", "clear", "deserialize")) {
  implicit def stemmer = MorphaStemmer
  override val info = "Enter a single sentence to be parsed."

  val urlCodec = new URLCodec

  lazy val clearParser = new ClearParser()
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
      case "clear" => clearParser
      case "malt" => maltParser
      case "deserialize" => deserializeParser
    }

  override def config[A](req: unfiltered.request.HttpRequest[A], tool: String) = {
    val pattern =
      if (req.parameterNames contains "pattern") req.parameterValues("pattern").headOption
      else None
    config(
      pattern,
      req.parameterNames.contains("collapsed"),
      req.parameterNames.contains("collapseNounGroups"),
      req.parameterNames.contains("collapsePrepOf"),
      req.parameterNames.contains("collapseWeakLeaves"))
  }

  def config(pattern: Option[String], collapsed: Boolean, collapseNounGroups: Boolean, collapsePrepOf: Boolean, collapseWeakLeaves: Boolean): String = """
    pattern: <input name="pattern" type="input" size="60" value="""" + pattern.getOrElse("") + """" /><br />
    <input name="collapsed" type="checkbox" value="true" """ + (if (collapsed) """checked="true" """ else "") + """ /> Collapsed<br />
    <input name="collapseNounGroups" type="checkbox" value="true" """ + (if (collapseNounGroups) """checked="true" """ else "") + """/> Collapse Noun Groups<br />
    <input name="collapsePrepOf" type="checkbox" value="true" """ + (if (collapsePrepOf) """checked="true" """ else "") + """/> Collapse Prep Of<br />
    <input name="collapseWeakLeaves" type="checkbox" value="true" """ + (if (collapseWeakLeaves) """checked="true" """ else "") + """/> Collapse Weak Leaves<br />
    <br />"""

  def whatswrongImage(graph: DependencyGraph) = {
    import visualize.Whatswrong._
    val b64 = implicitly[CanWrite[DependencyGraph, Base64String]].write(graph)
    "<img src=\"data:image/png;base64," + b64.string + "\">"
  }

  override def post[A](tool: String, text: String, params: Map[String, String]) = {
    val parser = getParser(tool)
    val pattern = ""
    var (parseTime, graph) = parser.synchronized {
      Timing.time(
        parser match {
          case parser: DependencyParser =>
            val graph = parser.dependencyGraph(text)
            if (/*params.getOrElse("collapsed", "")*/"true" == "true")
              graph.collapse
            else
              graph
        })
    }

    if (params.get("collapseNounGroups").getOrElse("") == "true") {
      graph = graph.collapseNounGroups()
    }

    if (params.get("collapsePrepOf").getOrElse("") == "true") {
      graph = graph.collapseNNPOf
    }

    if (params.get("collapseWeakLeaves").getOrElse("") == "true") {
      graph = graph.collapseWeakLeaves
    }

    val (nodes, edges) = if ((params.keys contains "pattern") && !params("pattern").isEmpty) {
      val pattern = DependencyPattern.deserialize(params("pattern").trim)
      val matches = pattern(graph.graph)
      (for (m <- matches; v <- m.bipath.nodes) yield v,
        for (m <- matches; e <- m.bipath.edges) yield e)
    }
    else (List(), List())

    val dot = graph.dotWithHighlights(if (text.length > 100) text.substring(0, 100) + "..." else text, Set.empty, Set.empty)
    val base64Image = DotIntent.dotbase64(dot, "png")

    ("parse time: " + Timing.Milliseconds.format(parseTime),
      whatswrongImage(graph) + "<br/><br/>" +
      "<img src=\"data:image/png;base64," + base64Image + "\" /><br>" +
      "<pre>serialized: " + graph.serialize + "\n\n" +
      graph.graph.toString + "\n\n" +
      dot + "</pre>")
  }
}
