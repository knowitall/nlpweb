package edu.washington.cs.knowitall
package nlpweb

import org.scalatra._
import scalate.ScalateSupport

abstract class ToolFilter(val path: String, val tools: List[String]) extends BaseFilter {
  get("/*") {
    indexPage(tools.map(path + "/" + _.toString + "/"))
  }

  def title(params: Map[String, String]) = params(path) + " " + path
  def config(params: Map[String, String]) = ""
  def info: String

  get("/" + path + "/:" + path + "/*") {
    if (!tools.contains(params(path))) pass
    basicPage(
      name = title(params), 
      info = info,
      text = "", 
      config = config(params),
      stats = "",
      result = "")
  }

  get("/" + path + "/:" + path + "/:text") {
    if (!tools.contains(params(path))) pass
    basicPage(
      name = title(params), 
      info = info,
      text = params("text"), 
      config = config(params),
      stats = "",
      result = "")
  }

  def doPost(params: Map[String, String]): (String, String)

  def buildTable(header: List[String], rows: Iterable[List[String]]) =
    buildColoredTable(header, rows.map{ items => (None, items) })

  def buildColoredTable(header: List[String], rows: Iterable[(Option[String], List[String])]) =
    "<table>" + 
      "<tr>" + header.map("<th>" + _ + "</th>").mkString("") + "</tr>" +
      rows.map{ case (color, items) => "<tr>" + items.map("<td"+color.map(" style=\"background-color: " + _ + "\")").getOrElse("")+">" + _ + "</td>").mkString("") + "</tr>" }.mkString("") + 
    "</table>"

  post("/" + path + "/:" + path + "/*") {
    if (!tools.contains(params(path))) pass
    val (stats, result) = doPost(params)
    basicPage(
      name = title(params), 
      info = info,
      text = params("text"), 
      config = config(params), 
      stats = stats,
      result = result)
  }
}
