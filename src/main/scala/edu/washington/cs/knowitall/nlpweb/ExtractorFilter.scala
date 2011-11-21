package edu.washington.cs.knowitall
package nlpweb

import common._

import org.scalatra._
import scalate.ScalateSupport
import java.net.URL

import collection.JavaConversions._

import org.apache.commons.io.IOUtils

import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter

import edu.washington.cs.knowitall.nlpweb.Common._

import edu.washington.cs.knowitall.Sentence
import edu.washington.cs.knowitall.util.DefaultObjects
import edu.washington.cs.knowitall.nlp.OpenNlpSentenceChunker
import edu.washington.cs.knowitall.stemmer.{Stemmer, MorphaStemmer, PorterStemmer}
import edu.washington.cs.knowitall.extractor._
import edu.washington.cs.knowitall.nlp.extraction._
import edu.washington.cs.knowitall.nlp.ChunkedSentence

class ExtractorFilter extends ToolFilter("extractor", List("reverb", "nesty", "r2a2", "relnoun", "all")) {
  override val info = "Enter sentences from which to extract relations, one per line."
  lazy val sentenceChunker = new OpenNlpSentenceChunker()
  lazy val sentenceDetector = DefaultObjects.getDefaultSentenceDetector()

  lazy val reverbExtractor = new ReVerbExtractor()
  lazy val nestyExtractor = new BinaryNestedExtractor()
  lazy val r2a2Extractor = new R2A2()
  lazy val relnounExtractor =  new RelationalNounExtractor()
  lazy val omniExtractor = new OmniExtractor()

  implicit def sentence2chunkedSentence(sentence: Sentence): ChunkedSentence = sentence.toChunkedSentence

  def getExtractor(name: String): Sentence=>Iterable[Any] = {
    s: Sentence => name match {
      case "reverb" => reverbExtractor.extract(s)
      case "nesty" => nestyExtractor.extract(s)
      case "r2a2" => r2a2Extractor.extract(s)
      case "relnoun" => relnounExtractor.extract(s)
      case "all" => omniExtractor.extract(s)
    }
  }

  override def doPost(params: Map[String, String]) = {
    def chunk(string: String) = sentenceChunker.synchronized {
      sentenceDetector.sentDetect(string).map { 
        string => new Sentence(sentenceChunker.chunkSentence(string), string)
      }
    }

    val extractor = getExtractor(params("extractor"))
    val text = params("text")

    val (chunkTime, chunked) = timed(chunk(text))
    val (extractionTime, extractions) = timed(chunked.flatMap(extractor(_)))
    ("chunking: " + Timing.format(chunkTime) + "\n" +
      "extracting: " + Timing.format(extractionTime),
      "<pre>" + extractions.size + " extraction(s)\n" + extractions.mkString("\n") + "</pre>")
  }
}
