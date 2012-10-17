package edu.washington.cs.knowitall
package nlpweb

import org.scalatra.ScalatraFilter
import org.scalatra.scalate.ScalateSupport

trait BaseFilter extends ScalatraFilter with ScalateSupport {

  def buildTemplatePath(name: String) = 
    "/WEB-INF/scalate/templates/" + name + ".jade"

  def basicPage(name: String, info: String = "", text: String, config: String="", stats: String="", result: String) {
    contentType = "text/html"
    templateEngine.layout(buildTemplatePath("basic"), 
      Map(
        "name" -> name, 
        "info" -> info,
        "text" -> text, 
        "config" -> config, 
        "stats" -> stats,
        "result" -> result))
  }

  def simplePage(name: String, info: String, text: String, result: String) { 
    basicPage(
      name=name, 
      info=info,
      text=text, 
      config="", 
      result="<pre>"+result+"</pre>")
  }

  def indexPage(pages: List[String]) {
    contentType = "text/html"
    templateEngine.layout(buildTemplatePath("index"), 
      Map("pages" -> pages))
  }

  def errorPage(message: String) {
    contentType = "text/html"
    templateEngine.layout(buildTemplatePath("error"), 
      Map("message" -> message))
  }
}
