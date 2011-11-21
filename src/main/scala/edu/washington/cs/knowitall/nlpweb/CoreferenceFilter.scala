package edu.washington.cs.knowitall
package nlpweb

import tool.coref._
import common._

import org.scalatra._
import java.net.URL
import scalate.ScalateSupport

import nlpweb.Common._

class CoreferenceFilter extends ToolFilter("coref", List("stanford")) {
  override val info = "Enter a document to be coreference resolved."
  lazy val stanfordCoref = new StanfordCoreferenceResolver()

  def resolvers = tools
  def getResolver(resolver: String): CoreferenceResolver =
    resolver match {
      case "stanford" => stanfordCoref
    }

  override def doPost(params: Map[String, String]) = {
    val resolver = getResolver(params("coref"))
    val text = params("text")

    val (mentionTime, mentions) = timed(resolver.mentions(text))
    val (resolveTime, resolved) = timed(resolver.resolve(text, (o,r)=>o+"["+r+"]"))
    val displayMentions = mentions.map{ case (k, v) => (k, v.toSet.filter(_ != k))}.filter(_._2.size > 0).toList.sortBy(_._2.size)
    ("time: " + Timing.format(mentionTime + resolveTime),
     "<pre style=\"white-space: pre-wrap;>mentions:\">\n" + displayMentions.map{case (a,b) => a + "\n" + b.map(" "*4+_).mkString("\n")}.mkString("\n") + "\n\nresolved:\n" + resolved + "</pre>")
  }
}
