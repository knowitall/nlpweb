package edu.washington.cs.knowitall
package nlpweb

import common._

import org.scalatra._
import java.net.URL
import scalate.ScalateSupport

import scala.collection.JavaConversions._

import org.apache.commons.io.IOUtils

import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter

import edu.washington.cs.knowitall.nlpweb.Common._

import edu.washington.cs.knowitall.Sentence
import edu.washington.cs.knowitall.util.DefaultObjects
import edu.washington.cs.knowitall.stemmer.{Stemmer, MorphaStemmer, PorterStemmer}
import edu.washington.cs.knowitall.tool.parse.{StanfordParser, OpenNlpParser, ConstituencyParser}
import edu.washington.cs.knowitall.tool.parse.graph._

class ConstituencyParserFilter extends ToolFilter("constituency", List("stanford", "opennlp")) {
  override val info = "Enter a single sentence to be parsed."
  lazy val stanfordParser = new StanfordParser()
  lazy val openNlpParser = new OpenNlpParser()

  val parsers = tools
  def getParser(parser: String): ConstituencyParser =
    parser match {
      case "stanford" => stanfordParser
      case "opennlp" => openNlpParser
    }

  override def doPost(params: Map[String, String]) = {
    val parser = getParser(params("constituency"))
    val input = params("text")
    var (parseTime, graph) = parser.synchronized {
      timed(parser.parse(input))
    }

    val buffer = new StringBuffer()
    graph.printDOT(buffer)
    val dot = buffer.toString
      .replaceAll("""\\n""", "")
      .replaceAll("""\s+""", " ")
      .replaceAll("\"", """%22""")

    ("parse time: " + Timing.format(parseTime),
     "<img src=\"" + servletContext.getContextPath + "/dot/png/" + dot + "\" />")
  }
}
