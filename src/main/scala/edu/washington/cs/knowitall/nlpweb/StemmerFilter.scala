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
import edu.washington.cs.knowitall.tool.stem.{Stemmer, MorphaStemmer, PorterStemmer, LovinsStemmer, PaiceStemmer}
import edu.washington.cs.knowitall.extractor.{ReVerbExtractor, R2A2, RelationalNounExtractor, OmniExtractor, NestedExtractor}

class StemmerFilter extends ToolFilter("stemmer", List("morpha", "porter", "paice", "lovins")) {
  override val info = "Enter tokens to stem, seperated by whitespace."

  lazy val morphaStemmer = new MorphaStemmer
  lazy val porterStemmer = new PorterStemmer
  lazy val paiceStemmer = new PaiceStemmer
  lazy val lovinsStemmer = new LovinsStemmer

  def getStemmer(stemmer: String): Stemmer =
    stemmer match {
      case "morpha" => morphaStemmer
      case "porter" => porterStemmer
      case "paice" => paiceStemmer
      case "lovins" => lovinsStemmer
    }

  override def doPost(params: Map[String, String]) = {
    val stemmer = getStemmer(params("stemmer"))
    val text = params("text")
    ("",
      text.split("\n").map(line => 
        line.split("\\s+").map(stemmer.stem(_)).dropWhile(_ == null).mkString(" ")).mkString("\n"))
  }
}
