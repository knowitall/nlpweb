package edu.knowitall
package nlpweb
package tool

import scala.annotation.migration
import scala.collection.JavaConversions.asJavaCollection

import common.Timing
import edu.knowitall.nlpweb.DotIntent
import edu.knowitall.nlpweb.ToolIntent
import edu.knowitall.nlpweb.visualize.Whatswrong.CanWrite
import edu.knowitall.tool.parse.ClearParser
import edu.knowitall.tool.parse.DependencyParser
import edu.knowitall.tool.parse.MaltParser
import edu.knowitall.tool.parse.RemoteDependencyParser
import edu.knowitall.tool.parse.StanfordParser
import edu.knowitall.tool.parse.graph.DependencyGraph
import edu.knowitall.tool.parse.graph.DependencyPattern
import edu.knowitall.tool.stem.MorphaStemmer
import visualize.Whatswrong.Base64String
import visualize.Whatswrong.CanWrite
import visualize.Whatswrong.writeGraphic2Base64

object ParserIntent
extends ToolIntent[DependencyParser]("parser",
    List(
      "stanford" -> "StanfordParser",
      "malt" -> "MaltParser",
      "clear" -> "ClearParser",
      "deserialize" -> "DeserializeParser")) {
  implicit def stemmer = MorphaStemmer
  override val info = "Enter a single sentence to be parsed."

  def constructors: PartialFunction[String, DependencyParser] = {
    case "StanfordParser" => new StanfordParser()
    case "MaltParser" => new MaltParser()
    case "ClearParser" => new ClearParser()
    case "DeserializeParser" => new DependencyParser {
      override def dependencyGraph(pickled: String) =
        DependencyGraph.deserialize(pickled)

      override def dependencies(pickled: String) =
        DependencyGraph.deserialize(pickled).dependencies
    }
  }
  override def remote(url: java.net.URL) = new RemoteDependencyParser(url.toString)

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
    try {
      import visualize.Whatswrong._
      val b64 = implicitly[CanWrite[DependencyGraph, Base64String]].write(graph)
      "<img src=\"data:image/png;base64," + b64.string + "\">"
    }
    catch {
      case e: Throwable => System.err.println("Could not build image for: " + graph.serialize); ""
    }
  }

  override def post[A](shortToolName: String, text: String, params: Map[String, String]) = {
    val parser = getTool(nameMap(shortToolName))
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

    val (matches, (nodes, edges)) = if ((params.keys contains "pattern") && !params("pattern").isEmpty) {
      val pattern = DependencyPattern.deserialize(params("pattern").trim)
      val matches = pattern(graph.graph)
      val nodes = for (m <- matches; v <- m.bipath.nodes) yield v
      val edges = for (m <- matches; e <- m.bipath.edges) yield e
      (matches, (nodes, edges))
    }
    else (List(), (List(), List()))

    val dot = graph.dotWithHighlights(if (text.length > 100) text.substring(0, 100) + "..." else text, nodes.toSet, edges.toSet)
    val base64Image = DotIntent.dotbase64(dot, "png")

    ("parse time: " + Timing.Milliseconds.format(parseTime),
      whatswrongImage(graph) + "<br/><br/>" +
      "<img src=\"data:image/png;base64," + base64Image + "\" /><br>" +
      "<pre>" + matches.map(m => "match with node groups (" + m.nodeGroups.mkString(" ") +
      ") and edge groups (" + m.edgeGroups.mkString(" ") + ")<br>").mkString("") + "<br>" +
      graph.text + "\n\n" + graph.serialize + "\n\n" +
      graph.graph.toString + "\n\n" +
      dot + "</pre>")
  }
}
