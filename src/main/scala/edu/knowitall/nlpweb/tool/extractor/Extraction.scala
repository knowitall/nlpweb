package edu.knowitall.nlpweb.tool.extractor

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.tokenize.Token

case class Part private (string: String, intervals: Iterable[Interval]) {
  def offsets(tokens: Seq[Token]) = {
    intervals.map(interval => Interval.open(tokens(interval.start).offsets.start, tokens(interval.last).offsets.end))
  }
}
object Part {
  def create(string: String, intervals: Iterable[Interval]) = {
    def collapse(intervals: List[Interval], result: List[Interval]): List[Interval] = intervals match {
      case Nil => result
      case current :: tail => result match {
        case Nil => collapse(tail, List(current))
        case last :: xs if last borders current => collapse(tail, (last union current) :: xs)
        case last :: xs => collapse(tail, current :: last :: xs)
      }
    }

    val sorted = intervals.toList.sorted
    new Part(string, collapse(sorted, List.empty))
  }
}
case class SemanticPart(semantics: String, part: Part) {
  def displayString = semantics + ":\"" + part.string + "\""
}
case class Attribute(string: String)
object ActiveAttribute extends Attribute("active")
object PassiveAttribute extends Attribute("passive")
object NegativeAttribute extends Attribute("negative")
case class Extraction(extractor: String, context: Option[Part], attributes: Seq[Attribute], arg1: Part, rel: Part, arg2s: Seq[Part], semanticArgs: Seq[SemanticPart], conf: Double) {
  def arg2 = {
    val allArg2s = arg2s ++ semanticArgs.map(_.part)
    Part.create(allArg2s.iterator.map(_.string).mkString("; "), allArg2s.flatMap(_.intervals))
  }
  def confidence = conf
  def span = Interval.span(rel.intervals ++ arg1.intervals ++ arg2s.flatMap(_.intervals))
}

object Extraction {
  def fromTriple(extractor: String, context: Option[Part], arg1: Part, rel: Part, arg2: Part, conf: Double) = {
    this(extractor, context, Seq.empty, arg1, rel, Seq(arg2), Seq.empty, conf)
  }
}