package edu.knowitall.nlpweb.tool.extractor

import edu.knowitall.srl.SrlExtractor
import edu.knowitall.srl.confidence.SrlConfidenceFunction
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.ollie.confidence.OllieConfidenceFunction
import edu.knowitall.nlpweb.tool.ParserIntent
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.chunkedextractor.ReVerb
import edu.knowitall.nlpweb.tool.ChunkerIntent
import edu.knowitall.chunkedextractor.R2A2
import edu.knowitall.chunkedextractor.Relnoun
import edu.knowitall.nlpweb.tool.StemmerIntent
import edu.knowitall.nlpweb.tool.SrlIntent
import edu.knowitall.chunkedextractor.Nesty
import edu.knowitall.tool.parse.DependencyParser

object Extractors {
  abstract class Extractor(val name: String) {
    def extract(sentence: String): Seq[Extraction]
    def apply(sentence: String) = extract(sentence)
  }

  class Ollie(name: String, parser: DependencyParser) extends Extractor(name) {
    import edu.knowitall.openparse.extract.Extraction.{ Part => OlliePart }
    import edu.knowitall.ollie._

    lazy val ollie = new edu.knowitall.ollie.Ollie()
    lazy val ollieConf = OllieConfidenceFunction.loadDefaultClassifier()

    def olliePart(extrPart: OlliePart) = Part.create(extrPart.text, extrPart.nodes.map(_.indices))
    def ollieContextPart(extrPart: Context) = {
      Part.create(extrPart.text, Iterable(extrPart.interval))
    }

    def extract(sentence: String): Seq[Extraction] = {
      val dgraph = parser(sentence)
      val rawOllieExtrs = ollie.extract(dgraph).map { extr => (ollieConf(extr), extr) }.toSeq.sortBy(-_._1)
      rawOllieExtrs.map(_._2).map { extr =>
        Extraction.fromTriple(name, extr.extr.enabler.orElse(extr.extr.attribution) map ollieContextPart, olliePart(extr.extr.arg1), olliePart(extr.extr.rel), olliePart(extr.extr.arg2), ollieConf(extr))
      }
    }
  }

  object ReVerb extends Extractor("ReVerb") {
    import edu.knowitall.chunkedextractor.{ ExtractionPart => ChunkedPart }
    def reverbPart(extrPart: ChunkedPart[ChunkedToken]) = Part.create(extrPart.text, Some(extrPart.tokenInterval))

    lazy val chunker = ChunkerIntent.getTool("OpenNlpChunker")
    lazy val reverb = new ReVerb()

    def extract(sentence: String) = {
      val chunked = chunker(sentence)
      reverb.extractWithConfidence(chunked).map {
        case (conf, extr) =>
          Extraction.fromTriple(name, None, ReVerb.reverbPart(extr.extr.arg1), ReVerb.reverbPart(extr.extr.rel), ReVerb.reverbPart(extr.extr.arg2), conf)
      }.toSeq
    }
  }

  object R2A2 extends Extractor("R2A2") {
    import edu.knowitall.chunkedextractor.{ ExtractionPart => ChunkedPart }
    def reverbPart(extrPart: ChunkedPart[ChunkedToken]) = Part.create(extrPart.text, Some(extrPart.tokenInterval))

    lazy val chunker = ChunkerIntent.getTool("OpenNlpChunker")
    lazy val r2a2 = new R2A2()

    def extract(sentence: String) = {
      val chunked = chunker(sentence)
      r2a2.extractWithConfidence(chunked).map {
        case (conf, extr) =>
          Extraction.fromTriple(name, None, ReVerb.reverbPart(extr.extr.arg1), ReVerb.reverbPart(extr.extr.rel), ReVerb.reverbPart(extr.extr.arg2), conf)
      }.toSeq
    }
  }

  object Relnoun extends Extractor("Relnoun") {
    import edu.knowitall.chunkedextractor.{ ExtractionPart => ChunkedPart }

    lazy val relnoun = new Relnoun()
    lazy val chunker = ChunkerIntent.getTool("OpenNlpChunker")
    lazy val lemmatizer = StemmerIntent.getTool("MorphaStemmer")

    def extract(sentence: String) = {
      val chunked = chunker(sentence)
      val lemmatized = chunked map lemmatizer.stemToken
      relnoun.extract(lemmatized).map { extr =>
        Extraction.fromTriple(name, None, ReVerb.reverbPart(extr.extr.arg1), ReVerb.reverbPart(extr.extr.rel), ReVerb.reverbPart(extr.extr.arg2), 0.9)
      }.toSeq
    }
  }

  object Nesty extends Extractor("Nesty") {
    import edu.knowitall.chunkedextractor.{ ExtractionPart => ChunkedPart }

    lazy val nesty = new Nesty()
    lazy val chunker = ChunkerIntent.getTool("OpenNlpChunker")
    lazy val lemmatizer = StemmerIntent.getTool("MorphaStemmer")

    def extract(sentence: String) = {
      val chunked = chunker(sentence)
      val lemmatized = chunked map lemmatizer.stemToken
      nesty.extract(lemmatized).map { extr =>
        Extraction.fromTriple(name, None, ReVerb.reverbPart(extr.extr.arg1), ReVerb.reverbPart(extr.extr.rel), ReVerb.reverbPart(extr.extr.arg2), 0.9)
      }.toSeq
    }
  }

  object Srl {
    import edu.knowitall.srl._

    lazy val clearParser = ParserIntent.getTool("ClearParser")
    lazy val srlExtractor = new SrlExtractor(SrlIntent.clearSrl)
    lazy val srlConf = SrlConfidenceFunction.loadDefaultClassifier()

    def convert(inst: SrlExtractionInstance): Extraction = {
      val arg1 = inst.extr.arg1
      val arg2s: Map[Class[_], Seq[SrlExtraction.Argument]] = inst.extr.arg2s.groupBy(_.getClass)
      val conf = srlConf(inst)

      val vanillaArg2s = arg2s.getOrElse(classOf[SrlExtraction.Argument], Seq.empty)
      val vanillaArg2Parts = vanillaArg2s.map { arg2 =>
        Part.create(arg2.text, Seq(arg2.interval))
      }

      val semanticArg2Parts: Seq[SemanticPart] = arg2s.filter {
        case (key, value) =>
          key != classOf[SrlExtraction.Argument]
      }.flatMap {
        case (key, values) =>
          values.map { value =>
            val semantics = key match {
              case x if x == classOf[SrlExtraction.LocationArgument] => "spatial"
              case x if x == classOf[SrlExtraction.TemporalArgument] => "temporal"
              case x => throw new IllegalArgumentException("Unknown semantic argument type: " + x)
            }

            val part = Part.create(value.text, Seq(value.interval))
            SemanticPart(semantics, part)
          }
      }.toSeq

      val attributes = Seq(
        if (inst.extr.passive) Some(PassiveAttribute) else None,
        if (inst.extr.active) Some(ActiveAttribute) else None,
        if (inst.extr.negated) Some(NegativeAttribute) else None).flatten

      val context = {
        inst.extr.context.map { context =>
          val tokens = context.tokens
          val text = context.text
          Part.create(text, context.intervals)
        }
      }

      Extraction("Open IE 4",
        context = context,
        attributes = attributes,
        arg1 = Part.create(arg1.text, Seq(arg1.interval)),
        rel = Part.create(inst.extr.relation.text, Seq(inst.extr.relation.span)),
        arg2s = vanillaArg2Parts,
        semanticArgs = semanticArg2Parts,
        conf = conf)
    }

    object Triple extends Extractor("SRL Triples") {
      def extract(sentence: String): Seq[Extraction] = {
        val dgraph = clearParser(sentence)
        val srlExtractions = srlExtractor.synchronized {
          srlExtractor(dgraph) flatMap (_.triplize(true))
        }
        (srlExtractions map convert).map(_.copy(extractor = name))
      }
    }

    object Nary extends Extractor("SRL Nary") {
      def extract(sentence: String): Seq[Extraction] = {
        val dgraph = clearParser(sentence)
        val srlExtractions = srlExtractor.synchronized {
          srlExtractor(dgraph)
        }
        (srlExtractions map convert).map(_.copy(extractor = name))
      }
    }
  }
}