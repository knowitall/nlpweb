package edu.washington.cs.knowitall
package nlpweb
package tool

import common.Timing
import unfiltered.request.HttpRequest
import edu.washington.cs.knowitall.tool.sentence.OpenNlpSentencer
import edu.washington.cs.knowitall.tool.srl.ClearSrl
import edu.washington.cs.knowitall.tool.srl.Frame

object SrlIntent extends ToolIntent("srl", List("clear")) {
  override val info = "Enter a sentence text to be SRL-ed."
  type CompleteSrl = String=>Seq[Frame]

  lazy val clearSrl = new ClearSrl()

  def getSrl(name: String): CompleteSrl = name match {
    case "clear" =>
      (sentence: String) => {
        val graph = ParserIntent.clearParser.dependencyGraph(sentence)
        clearSrl(graph)
      }
  }

  case class FrameSet(sentence: String, frames: Seq[Frame])
  def buildTable(set: FrameSet) = {
    "<table><tr><th colspan=\"4\">" + set.sentence + "</th></tr>" + set.frames.map{
      case Frame(relation, arguments) =>
        "<tr><td>" + relation + "</td><td>" + arguments.mkString("; ") + "</td></tr>"
    }.mkString("\n") + "</table><br/><br/>"
  }

  override def post[A](tool: String, text: String, params: Map[String, String]) = {
    val srl = getSrl(tool)

    val (srlTime, frames) = Timing.time(srl(text))
    val frameSets = Seq(FrameSet(text, frames))
    ("time: " + Timing.Milliseconds.format(srlTime),
    "<p>" + frameSets.flatMap(_.frames).size + " frame(s):</p>" + frameSets.map(buildTable(_)).mkString("\n"))
  }
}