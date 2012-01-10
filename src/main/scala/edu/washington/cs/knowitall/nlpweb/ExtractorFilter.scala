package edu.washington.cs.knowitall
package nlpweb

import common._

import org.scalatra._
import scalate.ScalateSupport
import java.net.URL

import scala.collection.JavaConversions._

import org.apache.commons.io.IOUtils

import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter

import edu.washington.cs.knowitall.Sentence
import edu.washington.cs.knowitall.util.DefaultObjects
import edu.washington.cs.knowitall.nlp.OpenNlpSentenceChunker
import edu.washington.cs.knowitall.stemmer.{ Stemmer, MorphaStemmer, PorterStemmer }
import edu.washington.cs.knowitall.extractor._
import edu.washington.cs.knowitall.nlp.extraction._
import edu.washington.cs.knowitall.nlp.ChunkedSentence

class ExtractorFilter extends ToolFilter("extractor", List("nesty", "r2a2", "relnoun", "reverb")) {
  override val info = "Enter sentences from which to extract relations, one per line."
  lazy val sentenceChunker = new OpenNlpSentenceChunker()
  lazy val sentenceDetector = DefaultObjects.getDefaultSentenceDetector()

  lazy val reverbExtractor = new ReVerbExtractor()
  lazy val nestyExtractor = new BinaryNestedExtractor()
  lazy val r2a2Extractor = new R2A2()
  lazy val relnounExtractor = new RelationalNounExtractor()

  implicit def sentence2chunkedSentence(sentence: Sentence): ChunkedSentence = sentence.toChunkedSentence

  def getExtractor(name: String): Sentence => List[(String, Iterable[(String, String, String)])] = {
    def triple(extr: ChunkedBinaryExtraction) =
      (extr.getArgument1.getText, extr.getRelation.getText, extr.getArgument2.getText)
    s: Sentence => List((name, name match {
      case "reverb" => reverbExtractor.extract(s).map(triple(_))
      case "nesty" => nestyExtractor.extract(s).map(triple(_))
      case "r2a2" => r2a2Extractor.extract(s).map(triple(_))
      case "relnoun" => relnounExtractor.extract(s).map(triple(_))
    }))
  }
  
  override def config(params: Map[String, String]): String = {
    val currentExtractor = params("extractor")
    (for (extractor <- this.tools) yield {
      "<input name=\"check_"+extractor+"\" type=\"checkbox\" value=\"true\"" + (if (extractor == currentExtractor || params.get("check_" + extractor) == Some("true")) """checked="true" """ else "") + " /> "+extractor+"<br />"
    }).mkString("\n")
  }

  override def doPost(params: Map[String, String]) = {
    def chunk(string: String) = sentenceChunker.synchronized {
      sentenceDetector.sentDetect(string).map {
        string => new Sentence(sentenceChunker.chunkSentence(string), string)
      }
    }
    
    def buildTable(extractions: (String, Iterable[(String, String, String)])) = {
      "<table><tr><th colspan=\"3\">"+extractions._1 + " extractions"+"</th></tr>" + extractions._2.map { extr => 
        "<tr><td>"+extr._1+"</td><td>"+extr._2+"</td><td>"+extr._3+"</td></tr>"
      }.mkString("\n")+"</table><br/><br/>"
    }

    val extractorName = params("extractor")
    val text = params("text")
    
    // create an extractor that extracts for all checked extractors
    def extractor(sentence: Sentence) = 
      for { 
        key <- params.keys; if key.startsWith("check_") 
        extrs <- getExtractor(key.drop(6))(sentence)
      } yield (extrs)

    val (chunkTime, chunked) = Timing.time(chunk(text))
    val (extractionTime, extractions) = Timing.time(chunked.flatMap(extractor(_)))
    ("chunking: " + Timing.Milliseconds.format(chunkTime) + "\n" +
      "extracting: " + Timing.Milliseconds.format(extractionTime),
      "<p>" + extractions.map(_._2).flatten.size + " extraction(s):</p>" + extractions.map(buildTable(_)).mkString("\n"))
  }
}
