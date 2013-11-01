package edu.knowitall
package nlpweb
package tool

import common.Timing
import unfiltered.request.HttpRequest
import edu.knowitall.tool.srl.Frame
import edu.knowitall.tool.srl.FrameHierarchy
import edu.knowitall.tool.parse.graph.DependencyGraph
import edu.knowitall.tool.srl.RemoteSrl
import edu.knowitall.tool.parse.DependencyParser
import edu.knowitall.tool.srl.Srl

import scala.util.control.Exception

case class SrlPackage(frames: Seq[Frame], graph: DependencyGraph)
abstract class CompleteSrl {
  def apply(sentence: String): SrlPackage
}
object CompleteSrl {
  def from(parser: DependencyParser, srl: Srl) = {
    new CompleteSrl {
      override def apply(sentence: String) = {
        val graph = (Exception.catching(classOf[Exception]) opt DependencyGraph.deserialize(sentence)) match {
          case Some(graph) => graph
          case None => parser(sentence)
        }
        SrlPackage(srl(graph), graph)
      }
    }
  }
}

object SrlIntent
extends ToolIntent[CompleteSrl]("srl", List("clear" -> "ClearSrl")) {

  override val info = "Enter a sentence text to be SRL-ed."

  val clearSrl = NlpWeb.remote.flatMap { remote =>
    if (remote.paths contains "/clear/srl") {
      Some(new RemoteSrl(remote.toolUrl("/clear/srl").toString))
    }
    else {
      None
    }
  }

  // lazy val clearSrl = new ClearSrl()
  def constructors= PartialFunction.empty[String, CompleteSrl] /* = {
    case "ClearSrl" =>
      val clearParser = ParserIntent.getTool("ClearParser")
      CompleteSrl.from(clearParser, clearSrl)
  }
  */
  override def remote(url: java.net.URL) = {
    val clearParser = ParserIntent.getTool("ClearParser")
    val srl = new RemoteSrl(url.toString)
    CompleteSrl.from(clearParser, srl)
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
      case e: Throwable => System.err.println("Could not build image for: " + srl); ""
    }
  }

  override def post[A](shortToolName: String, text: String, params: Map[String, String]) = {
    val srl = getTool(nameMap(shortToolName))

    val (srlTime, frames) = Timing.time(srl(text))
    val frameSets = Seq(frames)
    val hierarchySets = Seq(FrameHierarchy.fromFrames(frames.graph, frames.frames.toIndexedSeq))
    ("time: " + Timing.Milliseconds.format(srlTime),
    "<p>" + frameSets.flatMap(_.frames).size + " frame(s):</p>" + frameSets.map(buildTable(_)).mkString("\n") + hierarchySets.map(_.mkString("<p>", "<br />", "</p>")).mkString("\n") + (frameSets flatMap {case SrlPackage(frames, graph) => frames map (frame => image((graph, frame)))}).mkString("<p>", "<br />\n", "</p>") + "<p><pre>" + frames.frames.map(_.serialize).mkString("\n") + "</pre></p>")
  }
}
