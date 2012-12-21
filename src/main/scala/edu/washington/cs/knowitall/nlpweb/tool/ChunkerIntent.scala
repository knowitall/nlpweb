package edu.washington.cs.knowitall
package nlpweb
package tool

import scala.Array.canBuildFrom
import common.Timing
import edu.washington.cs.knowitall.nlpweb.ToolIntent
import edu.washington.cs.knowitall.tool.chunk.OpenNlpChunker
import edu.washington.cs.knowitall.tool.chunk.Chunker
import edu.washington.cs.knowitall.tool.chunk.ChunkedToken

class ChunkerIntent extends ToolIntent("chunker", List("opennlp")) {
  override val info = "Enter sentences to be chunked, one per line."
  lazy val opennlpChunker = new OpenNlpChunker()

  val chunkers = tools
  def getChunker(chunker: String): Chunker =
    chunker match {
      case "opennlp" => opennlpChunker
    }

  override def doPost(tool: String, text: String) = {
    val chunker = getChunker(tool)

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
