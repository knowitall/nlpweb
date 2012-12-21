package edu.washington.cs.knowitall
package nlpweb

import scala.Array.canBuildFrom

import common.Timing
import tool.chunk.{ChunkedToken, Chunker, OpenNlpChunker}

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
    val (chunkTime, chunkeds) = Timing.time(lines.map(chunker.chunk(_)))
    var colored = false
    ("time: " + Timing.Milliseconds.format(chunkTime),
    chunkeds.map { 
      chunked => buildColoredTable(List("strings", "postags", "chunks"), 
        chunked.map { 
          case ChunkedToken(chunk, postag, string, offset) => if (chunk.startsWith("B")) colored = !colored; (Some(if (chunk.startsWith("O")) "pink" else if (colored) "lightgrey" else "white"), List(string, postag, chunk))
        }) 
    }.mkString("<br>\n") )
  }
}
