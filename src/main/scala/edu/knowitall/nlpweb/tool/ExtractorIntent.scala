package edu.knowitall
package nlpweb
package tool

import scala.Array.canBuildFrom

import common.Timing
import edu.knowitall.nlpweb.ToolIntent
import edu.knowitall.nlpweb.tool.extractor.Extraction
import edu.knowitall.nlpweb.tool.extractor.Extractors.Extractor

object ExtractorIntent
extends ToolIntent[Extractor]("extractor",
    List(
        "reverb" -> "ReVerbExtractor",
        "r2a2" -> "R2A2Extractor",
        "relnoun" -> "RelnounExtractor",
        "nesty" -> "NestyExtractor",
        "ollie-clear" -> "OllieClearExtractor",
        "ollie-malt" -> "OllieMaltExtractor",
        "srl" -> "SrlExtractor",
        "srl-triple" -> "SrlTripleExtractor")) {
  override val info = "Enter sentences from which to extract relations, one per line."

  def constructors: PartialFunction[String, Extractor] = {
    case "ReVerbExtractor" => extractor.Extractors.ReVerb
    case "R2A2Extractor" => extractor.Extractors.R2A2
    case "RelnounExtractor" => extractor.Extractors.Relnoun
    case "NestyExtractor" => extractor.Extractors.Nesty
    case "OllieClearExtractor" => new extractor.Extractors.Ollie("Ollie-clear", ParserIntent.getTool("ClearParser"))
    case "OllieMaltExtractor" => new extractor.Extractors.Ollie("Ollie-malt", ParserIntent.getTool("ClearParser"))
    case "SrlExtractor" => extractor.Extractors.Srl.Nary
    case "SrlTripleExtractor" => extractor.Extractors.Srl.Triple
  }
  override def config[A](req: unfiltered.request.HttpRequest[A], tool: String) = {
    val currentExtractor = tool
    (for (extractor <- this.shortNames) yield {
      val key = "check_" + extractor
      "<input name=\"check_" + extractor + "\" type=\"checkbox\" value=\"true\"" + (if (extractor == currentExtractor || (req.parameterNames contains key) && req.parameterValues(key).headOption == Some("true")) """checked="true" """ else "") + " /> " + extractor + "<br />"
    }).mkString("\n")
  }

  override def post[A](tool: String, text: String, params: Map[String, String]) = {
    case class ExtractionSet(sentence: String, extractions: Seq[(String, Seq[Extraction])])

    def buildTable(set: ExtractionSet) = {
      "<table><tr>" +
        "<th colspan=\"6\">" + set.sentence + "</th>" +
      "</tr>" +
      "<tr>" + Seq("Confidence", "Context", "Attributes", "Argument 1", "Relation", "Argument 2(s)").map("<th>" + _ + "</th>").mkString("") + "</tr>" +
      set.extractions.map(extractions => "<tr><th colspan=\"6\">" + extractions._1 + " extractions" + "</th></tr>" +
      extractions._2.map {
        case Extraction(name, context, attributes, arg1, rel, arg2, semanticArg2s, conf) =>
          "<tr>" + Seq("%1.2f" format conf, context.map(_.string).getOrElse(""), attributes.map(_.string).mkString("; "), arg1.string, rel.string, (arg2.map(_.string) ++ semanticArg2s.map(semantic => semantic.semantics + ":" + semantic.part.string)).mkString("; ")).map("<td>" + _ + "</td>").mkString("") + "</tr>"
      }.mkString("\n")).mkString("\n") + "</table><br/><br/>"
    }

    val extractorName = tool

    // create an extractor that extracts for all checked extractors
    def extract(sentence: String) =
      (
        for {
          key <- params.keys; if key.startsWith("check_")
          extractor = getTool(nameMap(key.drop(6)))
          extrs = extractor(sentence)
      } yield (extractor.name -> extrs)).toSeq.sortBy { case (extr, extrs) => this.shortNames.toSeq.indexOf(extr) }

    val sentences = text.split("\n")
    val (extractionTime, extractions) = Timing.time(sentences.map(sentence => ExtractionSet(sentence, extract(sentence))))
    ("extracting: " + Timing.Milliseconds.format(extractionTime),
      "<p>" + extractions.map(_.extractions).flatten.map(_._2).flatten.size + " extraction(s):</p>" + extractions.map(buildTable(_)).mkString("\n"))
  }
}