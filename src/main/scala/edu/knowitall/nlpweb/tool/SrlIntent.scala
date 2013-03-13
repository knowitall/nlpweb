package edu.knowitall
package nlpweb
package tool

import common.Timing
import unfiltered.request.HttpRequest
import edu.knowitall.tool.sentence.OpenNlpSentencer
import edu.knowitall.tool.srl.ClearSrl
import edu.knowitall.tool.srl.Frame
import edu.knowitall.tool.srl.FrameHierarchy
import edu.knowitall.tool.parse.graph.DependencyGraph

object SrlIntent extends ToolIntent("srl", List("clear")) {

  override val info = "Enter a sentence text to be SRL-ed."
  case class SrlPackage(frames: Seq[Frame], graph: DependencyGraph)
  type CompleteSrl = String=>SrlPackage

  lazy val clearSrl = new ClearSrl()

  def getSrl(name: String): CompleteSrl = name match {
    case "clear" =>
      (sentence: String) => {
        val graph = ParserIntent.clearParser.dependencyGraph(sentence)
        SrlPackage(clearSrl(graph), graph)
      }
  }

  case class FrameSet(sentence: String, frames: Seq[Frame])
  def buildTable(set: SrlPackage) = {
    "<table><tr><th colspan=\"4\">" + set.graph.text + "</th></tr>" + set.frames.map{
      case Frame(relation, arguments) =>
        "<tr><td>" + relation + "</td><td>" + arguments.mkString("; ") + "</td></tr>"
    }.mkString("\n") + "</table><br/><br/>"
  }

  def image(srl: (DependencyGraph, Frame)) = {
    try {
      import visualize.Whatswrong._
      val b64 = implicitly[CanWrite[(DependencyGraph, Frame), Base64String]].write(srl)
      "<img src=\"data:image/png;base64," + b64.string + "\">"
    }
    catch {
      case e => System.err.println("Could not build image for: " + srl); ""
    }
  }

  override def post[A](tool: String, text: String, params: Map[String, String]) = {
    val srl = getSrl(tool)

    val (srlTime, frames) = Timing.time(srl(text))
    val frameSets = Seq(frames)
    val hierarchySets = Seq(FrameHierarchy.fromFrames(frames.graph, frames.frames.toIndexedSeq))
    ("time: " + Timing.Milliseconds.format(srlTime),
    "<p>" + frameSets.flatMap(_.frames).size + " frame(s):</p>" + frameSets.map(buildTable(_)).mkString("\n") + hierarchySets.map(_.mkString("<p>", "<br />", "</p>")).mkString("\n") + (frameSets flatMap {case SrlPackage(frames, graph) => frames map (frame => image((graph, frame)))}).mkString("<p>", "<br />\n", "</p>") + "<p><pre>" + frames.frames.map(_.serialize).mkString("\n") + "</pre></p>")
  }
}
