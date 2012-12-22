package edu.washington.cs.knowitall
package nlpweb

import edu.washington.cs.knowitall.nlpweb.persist.LogEntry
import unfiltered.request._
import unfiltered.filter.Intent
import unfiltered.response.Ok
import edu.washington.cs.knowitall.nlpweb.persist.Param

abstract class ToolIntent(val path: String, val tools: List[String]) extends BasePage {
  def intent = Intent {
    case req @ GET(Path(Seg(`path` :: tool :: Nil))) if (tools contains tool) =>
      Ok ~> basicPage(req,
        name = title(req),
        info = info,
        text = "",
        config = config(req, tool),
        stats = "",
        result = "")

    case req @ GET(Path(Seg(`path` :: tool :: text :: Nil))) if (tools contains tool) =>
      Ok ~> basicPage(req,
        name = title(req),
        info = info,
        text = text,
        config = config(req, tool),
        stats = "",
        result = "")

	case req@GET(Path(Seg(`path` :: Nil))) => {
	  Ok ~> indexPage(req, tools.map(path + "/" + _.toString + "/"))
	}

    case req @ POST(Path(Seg(`path` :: tool :: Nil))) if (tools contains tool) =>
      val params = req.parameterNames.map { case (k) => persist.Param(k, req.parameterValues(k).head) }.toIndexedSeq :+ Param("tool", tool)
      val entry = LogEntry(None, path, tool, params).persist()
      val text = req.parameterValues("text").headOption.getOrElse("")
      val (stats, result) = post(req, tool, text)
      Ok ~> basicPage(req,
        name = title(req),
        id = entry.id,
        info = info,
        text = text,
        config = config(req, tool),
        stats = stats,
        result = result)
  }

  def title[A](req: unfiltered.request.HttpRequest[A]) = req match {
    case Path(Seg(`path` :: tool :: xs)) => path + " " + tool
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
