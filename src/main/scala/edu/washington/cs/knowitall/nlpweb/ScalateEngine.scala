package edu.washington.cs.knowitall.nlpweb

trait ScalateEngine {
  import org.fusesource.scalate.TemplateEngine
  import org.fusesource.scalate.layout.DefaultLayoutStrategy
  implicit val engine = new TemplateEngine(Seq(new java.io.File("src/main/resources/templates")))
  engine.layoutStrategy = new DefaultLayoutStrategy(engine, "/layouts/default.jade")
}