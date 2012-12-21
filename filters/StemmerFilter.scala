package edu.washington.cs.knowitall
package nlpweb

import scala.Array.canBuildFrom

import edu.washington.cs.knowitall.tool.stem.{EnglishStemmer, MorphaStemmer, PorterStemmer, Stemmer}

class StemmerFilter extends ToolFilter("stemmer", List("morpha", "porter", "english")) {
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

  override def doPost(params: Map[String, String]) = {
    val stemmer = getStemmer(params("stemmer"))
    val text = params("text")
    ("",
      text.split("\n").map(line => 
        line.split("\\s+").map(stemmer.stem(_)).dropWhile(_ == null).mkString(" ")).mkString("\n"))
  }
}
