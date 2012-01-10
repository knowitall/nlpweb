package edu.washington.cs.knowitall
package nlpweb

import common._
import tool.postag._

import org.scalatra._
import scalate.ScalateSupport
import java.net.URL

import scala.collection.JavaConversions._

import org.apache.commons.io.IOUtils

import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter

class PosTaggerFilter extends ToolFilter("postagger", List("opennlp", "stanford")) {
  override val info = "Enter sentences to be part-of-speech tagged, one per line."
  lazy val postaggers = Map(
    "opennlp" -> new OpenNlpPosTagger(),
    "stanford" -> new StanfordPosTagger())

  override def doPost(params: Map[String, String]) = {
    val postagger = postaggers(params("postagger"))
    val text = params("text")

    val lines = text.split("\n")
    val (postagTime, postaggeds) = Timing.time(lines.map(postagger.postag(_)))
    ("time: " + Timing.Milliseconds.format(postagTime),
      postaggeds.map { 
        postagged => buildTable(List("string", "postag"), postagged.map { case (string, postag) => List(string, postag) })
      }.mkString("<br>\n"))
  }
}

