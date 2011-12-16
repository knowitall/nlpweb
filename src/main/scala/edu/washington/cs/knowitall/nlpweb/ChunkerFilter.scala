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

import nlpweb.Common._
import tool.chunk._

class ChunkerFilter extends ToolFilter("chunker", List("opennlp")) {
  override val info = "Enter sentences to be chunked, one per line."
  lazy val opennlpChunker = new OpenNlpChunker()

  val chunkers = tools
  def getChunker(chunker: String): Chunker =
    chunker match {
      case "opennlp" => opennlpChunker
    }

  override def doPost(params: Map[String, String]) = {
    val chunker = getChunker(params("chunker"))
    val text = params("text")

    val lines = text.split("\n")
    val (chunkTime, chunkeds) = timed(lines.map(chunker.chunk(_)))
    var colored = false
    ("time: " + Timing.format(chunkTime),
    chunkeds.map { 
      chunked => buildColoredTable(List("strings", "postags", "chunks"), 
        chunked.map { 
          case (string, (postag, chunk)) => if (chunk.startsWith("B")) colored = !colored; (Some(if (chunk.startsWith("O")) "pink" else if (colored) "lightgrey" else "white"), List(string, postag, chunk))
        }) 
    }.mkString("<br>\n") )
  }
}
