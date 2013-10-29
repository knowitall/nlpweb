package edu.knowitall
package nlpweb

import edu.knowitall.nlpweb.persist.LogEntry
import unfiltered.request._
import unfiltered.filter.Intent
import unfiltered.response.Ok
import edu.knowitall.nlpweb.persist.Param
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import java.net.URL
import org.apache.commons.lang.NotImplementedException

abstract class ToolIntent[T](val path: String, val toolNames: Seq[(String, String)]) extends BasePage {
  val logger = LoggerFactory.getLogger(this.getClass)

  for ((short, long) <- toolNames) {
    require(constructors.isDefinedAt(long), "No constructor for: " + long)
  }

  def constructors: PartialFunction[String, T]
  def remote(url: URL): T = throw new NotImplementedException("No remote implementation for: " + url)

  def shortNames = toolNames.iterator.map(_._1)
  def longNames = toolNames.iterator.map(_._2)
  val nameMap = toolNames.toMap

  var tools: Map[String, T] = Map.empty[String, T]
  def loadTool(name: String): T = {
    NlpWeb.remotes.get(name) match {
      case Some(url) =>
        logger.info("Connecting to remote " + name + ": " + url)
        remote(url)
      case None =>
        logger.info("Instantiating " + name)
        constructors(name)
    }
  }
  def getTool(name: String): T = tools.get(name) match {
    case Some(tool) => tool
    case None =>
      val loaded = loadTool(name)
      tools += name -> loaded
      loaded
  }

  def intent = Intent {
    case req @ GET(Path(Seg(`path` :: tool :: Nil))) if (shortNames contains tool) =>
      logger.info("Serving page: " + tool)
      Ok ~> basicPage(req,
        name = title(req),
        info = info,
        text = "",
        config = config(req, tool),
        stats = "",
        result = "")

    case req @ GET(Path(Seg(`path` :: tool :: text :: Nil))) if (shortNames contains tool) =>
      logger.info("Serving page '" + tool + "' with text: " + text)
      Ok ~> basicPage(req,
        name = title(req),
        info = info,
        text = text,
        config = config(req, tool),
        stats = "",
        result = "")

    case req@GET(Path(Seg(`path` :: Nil))) => {
      Ok ~> indexPage(req, shortNames.map(path + "/" + _.toString + "/").toList)
    }

    case req @ POST(Path(Seg(`path` :: tool :: Nil))) if (shortNames contains tool) =>
      val params = req.parameterNames.map { case (k) => persist.Param(k, req.parameterValues(k).head) }.toIndexedSeq :+ Param("tool", tool)
      val entry =
        try {
          Some(LogEntry(None, path, tool, params).persist())
        } catch {
          case e: Exception => ToolIntent.logger.error("Could not log request", e); None
        }
      val text = req.parameterValues("text").headOption.getOrElse("")
      def summarize(length: Int)(text: String) = {
        val cleaned = text.replaceAll("\n", " ")
        if (cleaned.length > length) cleaned.take(length) + "..."
        else cleaned
      }
      logger.info("Processing with '" + tool + "': " + summarize(40)(text))
      val (stats, result) = post(req, tool, text)
      Ok ~> basicPage(req,
        name = title(req),
        id = entry.flatMap(_.id),
        info = info,
        text = text,
        config = config(req, tool),
        stats = stats,
        result = result)
  }

  def title[A](req: unfiltered.request.HttpRequest[A]) = req match {
    case Path(Seg(`path` :: tool :: xs)) => "/" + path + "/" + tool
    case _ => "Unknown"
  }
  def config[A](req: unfiltered.request.HttpRequest[A], tool: String) = ""
  def info: String

  def post[A](req: HttpRequest[A], tool: String, text: String): (String, String) = {
    post(tool, text, req.parameterNames.map(name => (name, req.parameterValues(name).head)).toMap)
  }

  def post[A](tool: String, text: String, params: Map[String, String]): (String, String)

  def post[A](tool: String, params: Map[String, String]): (String, String) = {
    post(tool, params("text"), params)
  }

  def buildTable(header: List[String], rows: Iterable[List[String]]) =
    buildColoredTable(header, rows.map{ items => (None, items) })

  def buildColoredTable(header: List[String], rows: Iterable[(Option[String], List[String])]) =
    "<table>" +
      "<tr>" + header.map("<th>" + _ + "</th>").mkString("") + "</tr>" +
      rows.map{ case (color, items) => "<tr>" + items.map("<td"+color.map(" style=\"background-color: " + _ + "\")").getOrElse("")+">" + _ + "</td>").mkString("") + "</tr>" }.mkString("") +
    "</table>"
}

object ToolIntent {
  val logger = LoggerFactory.getLogger(this.getClass)
}
