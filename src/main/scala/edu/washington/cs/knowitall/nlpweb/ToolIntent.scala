package edu.washington.cs.knowitall
package nlpweb

import edu.washington.cs.knowitall.nlpweb.persist.LogEntry
import unfiltered.request._
import unfiltered.filter.Intent
import unfiltered.response.Ok

abstract class ToolIntent(val path: String, val tools: List[String]) extends BasePage {

  def intent = Intent {
    case req @ GET(Path(Seg(`path` :: tool :: Nil))) if (tools contains tool) =>
      Ok ~> basicPage(req,
        name = title(req),
        info = info,
        text = "",
        config = config(req),
        stats = "",
        result = "")

    case req @ GET(Path(Seg(`path` :: tool :: text :: Nil))) if (tools contains tool) =>
      Ok ~> basicPage(req,
        name = title(req),
        info = info,
        text = text,
        config = config(req),
        stats = "",
        result = "")

	case req@GET(Path(Seg(`path` :: Nil))) => {
	  Ok ~> indexPage(req, tools.map(path + "/" + _.toString + "/"))
	}

    case req @ POST(Path(Seg(`path` :: tool :: Nil))) if (tools contains tool) =>
      // LogEntry(None, path, params.iterator.map { case (k, v) => persist.Param(k, v) }.toIndexedSeq).persist()
      val text = req.parameterValues("text").headOption.getOrElse("")
      val (stats, result) = doPost(tool, text)
      Ok ~> basicPage(req,
        name = title(req),
        info = info,
        text = text,
        config = config(req),
        stats = stats,
        result = result)
  }

  def title[A](req: unfiltered.request.HttpRequest[A]) = req match {
    case Path(Seg(`path` :: tool :: xs)) => path + " " + tool
    case _ => "Unknown"
  }
  def config[A](req: unfiltered.request.HttpRequest[A]) = ""
  def info: String

  def doPost[A](tool: String, text: String): (String, String)

  def buildTable(header: List[String], rows: Iterable[List[String]]) =
    buildColoredTable(header, rows.map{ items => (None, items) })

  def buildColoredTable(header: List[String], rows: Iterable[(Option[String], List[String])]) =
    "<table>" +
      "<tr>" + header.map("<th>" + _ + "</th>").mkString("") + "</tr>" +
      rows.map{ case (color, items) => "<tr>" + items.map("<td"+color.map(" style=\"background-color: " + _ + "\")").getOrElse("")+">" + _ + "</td>").mkString("") + "</tr>" }.mkString("") +
    "</table>"
}
