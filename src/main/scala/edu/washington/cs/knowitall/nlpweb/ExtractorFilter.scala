package edu.washington.cs.knowitall
package nlpweb

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.iterableAsScalaIterable

import common.Timing
import edu.washington.cs.knowitall.argumentidentifier.ConfidenceMetric
import edu.washington.cs.knowitall.chunkedextractor.{BinaryExtractionInstance, Nesty, Relnoun}
import edu.washington.cs.knowitall.extractor.{R2A2, ReVerbExtractor}
import edu.washington.cs.knowitall.extractor.conf.ReVerbOpenNlpConfFunction
import edu.washington.cs.knowitall.nlp.ChunkedSentence
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction
import edu.washington.cs.knowitall.ollie.Ollie
import edu.washington.cs.knowitall.ollie.confidence.OllieIndependentConfFunction
import edu.washington.cs.knowitall.openparse
import edu.washington.cs.knowitall.openparse.OpenParse
import edu.washington.cs.knowitall.tool.chunk.{ChunkedToken, OpenNlpChunker}
import edu.washington.cs.knowitall.tool.parse.MaltParser
import edu.washington.cs.knowitall.tool.stem.{Lemmatized, MorphaStemmer}
import edu.washington.cs.knowitall.tool.tokenize.Token
import edu.washington.cs.knowitall.util.DefaultObjects

class ExtractorFilter extends ToolFilter("extractor", List("reverb", "relnoun", "nesty", "r2a2", "openparse", "ollie")) {
  override val info = "Enter sentences from which to extract relations, one per line."
  lazy val sentenceDetector = DefaultObjects.getDefaultSentenceDetector()

  lazy val parser = new MaltParser()

  lazy val ollieExtractor = new Ollie()
  lazy val ollieConfidence = OllieIndependentConfFunction.loadDefaultClassifier()

  lazy val openparseExtractor = {
    OpenParse.withDefaultModel()
  }
  lazy val reverbExtractor = new ReVerbExtractor()
  lazy val nestyExtractor = new Nesty()
  lazy val r2a2Extractor = new R2A2()
  lazy val relnounExtractor = new Relnoun()

  lazy val reverbConfidence = new ReVerbOpenNlpConfFunction()
  lazy val r2a2Confidence = new ConfidenceMetric()

  lazy val chunker = new OpenNlpChunker

  implicit def lemmatized2token(lemmatized: Lemmatized[ChunkedToken]): Token = {
    lemmatized.token
  }

  implicit def tokens2chunkedSentence(tokens: Seq[Lemmatized[ChunkedToken]]): ChunkedSentence = {
    new ChunkedSentence(tokens.map(_.token.string).toArray, tokens.map(_.token.postag).toArray, tokens.map(_.token.chunk).toArray)
  }

  def getExtractor(name: String): Seq[Lemmatized[ChunkedToken]] => List[(String, Iterable[(Double, (String, String, String))])] = {
    def triple(extr: ChunkedBinaryExtraction): (String, String, String) =
      (extr.getArgument1.getText, extr.getRelation.getText, extr.getArgument2.getText)
    def tripleChunked(inst: BinaryExtractionInstance[ChunkedToken]): (String, String, String) =
      (inst.extr.arg1.text, inst.extr.rel.text, inst.extr.arg2.text)
    def tripleOpenParse(extr: openparse.extract.DetailedExtraction) =
      (extr.arg1Text, extr.relText, extr.arg2Text)
    def tripleOllie(inst: ollie.OllieExtractionInstance) =
      (inst.extr.arg1.text, inst.extr.rel.text, inst.extr.arg2.text)

    s: Seq[Lemmatized[ChunkedToken]] => List((name, name match {
      case "ollie" => {
        val extrs = ollieExtractor.extract(parser.dependencyGraph(s.iterator.map(_.string).mkString(" "))).toList
        val confs = extrs map ollieConfidence.getConf
        confs zip (extrs map tripleOllie)
      }
      case "openparse" => openparseExtractor.extract(parser.dependencyGraph(s.iterator.map(_.string).mkString(" "))).toSeq.map(extr => (extr._1, tripleOpenParse(extr._2)))
      case "reverb" =>
        val extrs: List[ChunkedBinaryExtraction] = reverbExtractor.extract(s).toList
        val confs: List[Double] = extrs map reverbConfidence.getConf
        confs zip (extrs.map(triple(_)))
      case "nesty" => nestyExtractor.extract(s).map(ex => (0.5, tripleChunked(ex)))
      case "r2a2" =>
        val extrs: List[ChunkedBinaryExtraction] = r2a2Extractor.extract(s).toList
        val confs: List[Double] = extrs map reverbConfidence.getConf
        confs zip (extrs.map(triple(_)))
      case "relnoun" => relnounExtractor.extract(s).map(ex => (0.5, tripleChunked(ex)))
    }))
  }

  override def config(params: Map[String, String]): String = {
    val currentExtractor = params("extractor")
    (for (extractor <- this.tools) yield {
      "<input name=\"check_" + extractor + "\" type=\"checkbox\" value=\"true\"" + (if (extractor == currentExtractor || params.get("check_" + extractor) == Some("true")) """checked="true" """ else "") + " /> " + extractor + "<br />"
    }).mkString("\n")
  }

  override def doPost(params: Map[String, String]) = {
    case class ExtractionSet(sentence: String, extractions: Seq[(String, Iterable[(Double, (String, String, String))])])

    def chunk(string: String) = chunker.synchronized {
      sentenceDetector.sentDetect(string).map {
        string => chunker.chunk(string) map MorphaStemmer.instance.lemmatizeToken
      }
    }

    def buildTable(set: ExtractionSet) = {
      "<table><tr><th colspan=\"4\">" + set.sentence + "</th></tr>" + set.extractions.map(extractions => "<tr><th colspan=\"4\">" + extractions._1 + " extractions" + "</th></tr>" + extractions._2.map {
        case (conf, (arg1, rel, arg2)) =>
          "<tr><td>" + ("%1.2f" format conf) + "</td><td>" + arg1 + "</td><td>" + rel + "</td><td>" + arg2 + "</td></tr>"
      }.mkString("\n")).mkString("\n") + "</table><br/><br/>"
    }

    val extractorName = params("extractor")
    val text = params("text")

    // create an extractor that extracts for all checked extractors
    def extractor(sentence: Seq[Lemmatized[ChunkedToken]]) =
      (for {
        key <- params.keys; if key.startsWith("check_")
        extrs <- getExtractor(key.drop(6))(sentence)
      } yield (extrs)).toSeq.sortBy { case (extr, extrs) => this.tools.indexOf(extr) }

    val (chunkTime, chunked) = Timing.time(chunk(text))
    val (extractionTime, extractions) = Timing.time(chunked.map(chunked => ExtractionSet(chunked.getTokensAsString, extractor(chunked))))
    ("chunking: " + Timing.Milliseconds.format(chunkTime) + "\n" +
      "extracting: " + Timing.Milliseconds.format(extractionTime),
      "<p>" + extractions.map(_.extractions).flatten.map(_._2).flatten.size + " extraction(s):</p>" + extractions.map(buildTable(_)).mkString("\n"))
  }
}
