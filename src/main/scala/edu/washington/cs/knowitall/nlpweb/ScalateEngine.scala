package edu.washington.cs.knowitall.nlpweb

import org.fusesource.scalate.util.FileResourceLoader
import org.fusesource.scalate.util.Resource

trait ScalateEngine {
  import org.fusesource.scalate.TemplateEngine
  import org.fusesource.scalate.layout.DefaultLayoutStrategy
  implicit val engine = new TemplateEngine() {
  }

  engine.resourceLoader = new FileResourceLoader {
    override def resource(uri: String): Option[Resource] = {
      val url = this.getClass.getResource(uri)

      url match {
        case null => None
        case url => Some(Resource.fromURL(url))
      }
    }
  }
  engine.layoutStrategy = new DefaultLayoutStrategy(engine, "/layouts/default.jade")
}