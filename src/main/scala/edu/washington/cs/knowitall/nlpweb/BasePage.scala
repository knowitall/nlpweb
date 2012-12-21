package edu.washington.cs.knowitall
package nlpweb

import unfiltered.scalate.Scalate

trait BasePage {
  import org.fusesource.scalate.TemplateEngine
  import org.fusesource.scalate.layout.DefaultLayoutStrategy
  implicit val engine = new TemplateEngine(Seq(new java.io.File("src/main/resources/templates")))
  engine.layoutStrategy = new DefaultLayoutStrategy(engine, "/layouts/default.jade")

  def buildTemplatePath(name: String) =
    name + ".jade"

  def basicPage[A](req: unfiltered.request.HttpRequest[A], name: String, info: String = "", text: String, config: String="", stats: String="", result: String) = {
    Scalate(req, buildTemplatePath("basic"),
        "name" -> name,
        "info" -> info,
        "text" -> text,
        "config" -> config,
        "stats" -> stats,
        "result" -> result)
  }

  def simplePage[A](req: unfiltered.request.HttpRequest[A], name: String, info: String, text: String, result: String) = {
    basicPage(req,
      name=name,
      info=info,
      text=text,
      config="",
      result="<pre>"+result+"</pre>")
  }

  def indexPage[A](req: unfiltered.request.HttpRequest[A], pages: List[String]) = {
    Scalate(req, buildTemplatePath("index"),
      "pages" -> pages)
  }

  def errorPage[A](req: unfiltered.request.HttpRequest[A], message: String) = {
    Scalate(req, buildTemplatePath("error"),
      "message" -> message)
  }
}
