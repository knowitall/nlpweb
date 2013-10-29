package edu.knowitall
package nlpweb
package tool

import scala.Array.canBuildFrom

import common.Timing
import edu.knowitall.nlpweb.ToolIntent
import edu.knowitall.nlpweb.visualize.Whatswrong.CanWrite
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.chunk.Chunker
import edu.knowitall.tool.chunk.RemoteChunker
import edu.knowitall.tool.chunk.OpenNlpChunker
import visualize.Whatswrong.Base64String
import visualize.Whatswrong.writeGraphic2Base64

object ChunkerIntent extends ToolIntent[Chunker]("chunk", List("opennlp" -> "OpenNlpChunker")) {
  override val info = "Enter sentences to be chunked, one per line."
  lazy val opennlpChunker = new OpenNlpChunker()

  def constructors: PartialFunction[String, Chunker] = {
    case "OpenNlpChunker" => new OpenNlpChunker()
  }
  override def remote(url: java.net.URL) = new RemoteChunker(url.toString)

  def image(tokens: Seq[ChunkedToken]) = {
    import visualize.Whatswrong._
    val b64 = implicitly[CanWrite[Seq[ChunkedToken], Base64String]].write(tokens)
    "<img src=\"data:image/png;base64," + b64.string + "\">"
  }

  def config(pattern: Option[String], collapsed: Boolean, collapseNounGroups: Boolean, collapsePrepOf: Boolean, collapseWeakLeaves: Boolean): String = """
    patterns: <textarea name="pattern" cols="60" rows="20" value="""" + pattern.getOrElse("") + """" /><br />
    <br />"""

  override def post[A](shortToolName: String, text: String, params: Map[String, String]) = {
    val chunker = getTool(nameMap(shortToolName))

    val lines = text.split("\n")
    val (chunkTime, chunkeds) = Timing.time(lines.map(chunker.chunk(_)))
    var colored = false
    ("time: " + Timing.Milliseconds.format(chunkTime),
    chunkeds.map {
      chunked => buildColoredTable(List("strings", "postags", "chunks"),
        chunked.map {
          case ChunkedToken(chunk, postag, string, offset) => if (chunk.startsWith("B")) colored = !colored; (Some(if (chunk.startsWith("O")) "pink" else if (colored) "lightgrey" else "white"), List(string, postag, chunk))
        })
    }.mkString("<br>\n") + (chunkeds map image).mkString("<p>", "\n", "</p>") )
  }
}
