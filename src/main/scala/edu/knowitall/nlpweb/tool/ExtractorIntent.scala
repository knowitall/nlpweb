package edu.knowitall
package nlpweb
package tool

import scala.Array.canBuildFrom
import common.Timing
import edu.knowitall.chunkedextractor.BinaryExtractionInstance
import edu.knowitall.chunkedextractor.Nesty
import edu.knowitall.chunkedextractor.R2A2
import edu.knowitall.chunkedextractor.ReVerb
import edu.knowitall.chunkedextractor.Relnoun
import edu.knowitall.nlpweb.ToolIntent
import edu.knowitall.ollie.Ollie
import edu.knowitall.ollie.confidence.OllieConfidenceFunction
import edu.knowitall.openparse
import edu.knowitall.openparse.OpenParse
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.tokenize.Token
import knowitall.srl.SrlExtractor
import edu.knowitall.ollie.Attribution
import edu.knowitall.ollie.EnablingCondition
import edu.knowitall.openparse.extract.Extraction.ClausalComponent
import edu.knowitall.openparse.extract.Extraction.AdverbialModifier
import edu.knowitall.tool.chunk.Chunker

object ExtractorIntent extends ToolIntent("extractor", List("reverb", "relnoun", "nesty", "openparse", "ollie-clear", "ollie-malt", "srl")) {
  override val info = "Enter sentences from which to extract relations, one per line."

  lazy val ollieExtractor = new Ollie()
  lazy val ollieConfidence = OllieConfidenceFunction.loadDefaultClassifier()

  lazy val openparseExtractor = {
    OpenParse.withDefaultModel()
  }
  lazy val reverbExtractor = new ReVerb()
  lazy val nestyExtractor = new Nesty()
  lazy val r2a2Extractor = new R2A2()
  lazy val relnounExtractor = new Relnoun()

  lazy val srlExtractor = new SrlExtractor(SrlIntent.clearSrl)

  lazy val chunker = ChunkerIntent.opennlpChunker

  implicit def lemmatized2token(lemmatized: Lemmatized[ChunkedToken]): Token = {
    lemmatized.token
  }

  def getExtractor(name: String): Seq[Lemmatized[ChunkedToken]] => List[(String, Iterable[Extraction])] = {
    def tripleChunked(conf: Double)(inst: BinaryExtractionInstance[ChunkedToken]): Extraction = 
      new Extraction(conf, "", inst.extr.arg1.text, inst.extr.rel.text, inst.extr.arg2.text)
    def tripleOpenParse(conf: Double)(extr: openparse.extract.DetailedExtraction): Extraction = {
      val context = Seq(extr.modifier, extr.clausal).flatten map {
        case modifier: AdverbialModifier => "Mod: " + modifier.text
        case enabler: ClausalComponent => "Clause: " + enabler.text
      }
      new Extraction(conf, "", extr.arg1Text, extr.relText, extr.arg2Text)
    }
    def tripleOllie(conf: Double)(inst: ollie.OllieExtractionInstance): Extraction = {
      val context = Seq(inst.extr.attribution, inst.extr.enabler).flatten map {
        case attribution: Attribution => "Attr: " + attribution.text
        case enabler: EnablingCondition => "Cond: " + enabler.text
      }
      new Extraction(conf, context.mkString("; "), inst.extr.arg1.text, inst.extr.rel.text, inst.extr.arg2.text)
    }
    s: Seq[Lemmatized[ChunkedToken]] => List((name, name match {
      case "ollie-clear" => {
        val extrs = ollieExtractor.extract(ParserIntent.clearParser.dependencyGraph(s.iterator.map(_.string).mkString(" "))).toList
        val confs = extrs map ollieConfidence.getConf
        extrs map { extr => tripleOllie(ollieConfidence(extr))(extr) }
      }
      case "ollie-malt" => {
        val extrs = ollieExtractor.extract(ParserIntent.maltParser.dependencyGraph(s.iterator.map(_.string).mkString(" "))).toList
        extrs map { extr => tripleOllie(ollieConfidence(extr))(extr) }
      }
      case "openparse" => openparseExtractor.extract(ParserIntent.maltParser.dependencyGraph(s.iterator.map(_.string).mkString(" "))).toSeq.map { case(conf, extr) => tripleOpenParse(conf)(extr) }
      case "reverb" =>
        val extrs = reverbExtractor.extractWithConfidence(s.map(_.token)).toList
        extrs.map{ case (conf, extr) => tripleChunked(conf)(extr) }
      case "nesty" => nestyExtractor.extract(s).map(ex => tripleChunked(0.0)(ex))
      case "r2a2" =>
        val extrs = r2a2Extractor.extractWithConf(s.map(_.token)).toList
        extrs.map{ case (Some(conf), extr) => tripleChunked(conf)(extr) case _ => throw new IllegalArgumentException("No confidence function for R2A2.")}
      case "relnoun" => relnounExtractor.extract(s).map(ex => tripleChunked(0.0)(ex))
      case "srl" => {
        val parser = ParserIntent.clearParser
        val graph = parser.dependencyGraph(s.iterator.map(_.token.string).mkString(" "))
        srlExtractor.apply(graph).map { extraction =>
          new Extraction(0.0, "", extraction.arg1.text, extraction.relation.text, extraction.arg2s.map(_.text).mkString(", "))
        }
      }
    }))
  }

  override def config[A](req: unfiltered.request.HttpRequest[A], tool: String) = {
    val currentExtractor = tool
    (for (extractor <- this.tools) yield {
      val key = "check_" + extractor
      "<input name=\"check_" + extractor + "\" type=\"checkbox\" value=\"true\"" + (if (extractor == currentExtractor || (req.parameterNames contains key) && req.parameterValues(key).headOption == Some("true")) """checked="true" """ else "") + " /> " + extractor + "<br />"
    }).mkString("\n")
  }

  override def post[A](tool: String, text: String, params: Map[String, String]) = {
    case class ExtractionSet(sentence: String, extractions: Seq[(String, Iterable[Extraction])])

    def chunk(string: String) = chunker.synchronized {
      string.split("\n").map {
        string => Chunker.joinOf(chunker.chunk(string.trim)) map MorphaStemmer.lemmatizeToken
      }
    }

    def buildTable(set: ExtractionSet) = {
      "<table><tr>" +
        "<th colspan=\"5\">" + set.sentence + "</th>" +
      "</tr>" +
      "<tr>" + Seq("Confidence", "Context", "Argument 1", "Relation", "Argument 2(s)").map("<th>" + _ + "</th>").mkString("") + "</tr>" +
      set.extractions.map(extractions => "<tr><th colspan=\"5\">" + extractions._1 + " extractions" + "</th></tr>" +
      extractions._2.map {
        case Extraction(conf, context, arg1, rel, arg2) =>
          "<tr>" + Seq("%1.2f" format conf, context, arg1.string, rel.string, arg2.string).map("<td>" + _ + "</td>").mkString("") + "</tr>"
      }.mkString("\n")).mkString("\n") + "</table><br/><br/>"
    }

    val extractorName = tool

    // create an extractor that extracts for all checked extractors
    def extractor(sentence: Seq[Lemmatized[ChunkedToken]]) =
      (for { key <- params.keys; if key.startsWith("check_")
        extrs <- getExtractor(key.drop(6))(sentence)
      } yield (extrs)).toSeq.sortBy { case (extr, extrs) => this.tools.indexOf(extr) }

    val (chunkTime, chunked) = Timing.time(chunk(text))
    val (extractionTime, extractions) = Timing.time(chunked.map(chunked => ExtractionSet(chunked.map(_.token).mkString(" "), extractor(chunked))))
    ("chunking: " + Timing.Milliseconds.format(chunkTime) + "\n" +
      "extracting: " + Timing.Milliseconds.format(extractionTime),
      "<p>" + extractions.map(_.extractions).flatten.map(_._2).flatten.size + " extraction(s):</p>" + extractions.map(buildTable(_)).mkString("\n"))
  }
}

case class Extraction(conf: Double, context: String = "", arg1: Part, rel: Part, arg2: Part) {
  def this(conf: Double, context: String, arg1: String, rel: String, arg2: String) = this(conf, context, new Part(arg1), new Part(rel), new Part(arg2))
}
case class Part(string: String) 
