package edu.knowitall
package nlpweb.tool

import scala.Array.canBuildFrom
import edu.knowitall.tool.stem.{MorphaStemmer, PorterStemmer, EnglishStemmer, Stemmer}
import edu.knowitall.nlpweb.ToolIntent
import unfiltered.request.HttpRequest
import edu.knowitall.tool.stem.RemoteStemmer

object StemmerIntent
extends ToolIntent[Stemmer]("stem",
    List(
        "morpha" -> "MorphaStemmer",
        "porter" -> "PorterStemmer",
        "english" -> "EnglishStemmer")) {
  override val info = "Enter tokens to stem, seperated by whitespace."

  def constructors: PartialFunction[String, Stemmer] = {
    case "MorphaStemmer" => new MorphaStemmer()
    case "PorterStemmer" => new PorterStemmer()
    case "EnglishStemmer" => new EnglishStemmer()
  }
  
  override def remote(url: java.net.URL) = new RemoteStemmer(url.toString)

  override def post[A](shortToolName: String, text: String, params: Map[String, String]) = {
    val stemmer = getTool(nameMap(shortToolName))
    ("",
      text.split("\n").map(line =>
        line.split("\\s+").map(stemmer.stem(_)).dropWhile(_ == null).mkString(" ")).mkString("\n"))
  }
}
