package edu.washington.cs.knowitall
package nlpweb

import scala.Array.canBuildFrom
import common.Timing
import tool.tokenize.{OpenNlpTokenizer, StanfordTokenizer}
import org.scalatra.ScalatraFilter
import edu.washington.cs.knowitall.nlpweb.persist.Database
import edu.washington.cs.knowitall.nlpweb.persist.Tokens
import org.scalatra.UrlSupport

class LogFilter extends ScalatraFilter with UrlSupport {
  get("/log/:id") {
    val entry = Database.broker.readOnly() { session =>
      session.selectOne(Tokens.selectLogEntryById, "id" -> params("id"))
    }.getOrElse(throw new IllegalArgumentException("No such log: " + params("id")))

    val redirectParams = entry.params.map(_.toTuple).toMap
    redirect(url("/" + entry.path, redirectParams))
  }
}
