package edu.washington.cs.knowitall
package nlpweb.tool

import scala.Array.canBuildFrom
import edu.washington.cs.knowitall.tool.stem.{EnglishStemmer, MorphaStemmer, PorterStemmer, Stemmer}
import edu.washington.cs.knowitall.nlpweb.ToolIntent
import unfiltered.request.HttpRequest

class StemmerIntent extends ToolIntent("stemmer", List("morpha", "porter", "english")) {
  override val info = "Enter tokens to stem, seperated by whitespace."

  lazy val morphaStemmer = new MorphaStemmer
  lazy val porterStemmer = new PorterStemmer
  lazy val englishStemmer = new EnglishStemmer

  def getStemmer(stemmer: String): Stemmer =
    stemmer match {
      case "morpha" => morphaStemmer
      case "porter" => porterStemmer
      case "english" => englishStemmer
    }

  override def post[A](req: HttpRequest[A], tool: String, text: String) = {
    val stemmer = getStemmer(tool)
    ("",
      text.split("\n").map(line =>
        line.split("\\s+").map(stemmer.stem(_)).dropWhile(_ == null).mkString(" ")).mkString("\n"))
  }
}
