package edu.washington.cs.knowitall.nlpweb

import edu.washington.cs.knowitall.nlpweb.persist.Database
import edu.washington.cs.knowitall.nlpweb.tool.ChunkerIntent
import edu.washington.cs.knowitall.nlpweb.tool.ConstituencyParserIntent
import edu.washington.cs.knowitall.nlpweb.tool.ExtractorIntent
import edu.washington.cs.knowitall.nlpweb.tool.ParserIntent
import edu.washington.cs.knowitall.nlpweb.tool.PostaggerIntent
import edu.washington.cs.knowitall.nlpweb.tool.SentencerIntent
import edu.washington.cs.knowitall.nlpweb.tool.StemmerIntent
import edu.washington.cs.knowitall.nlpweb.tool.TokenizerIntent
import scopt.immutable.OptionParser
import unfiltered.filter.Intent
import unfiltered.jetty.ContextBuilder
import unfiltered.jetty.Http
import unfiltered.request.GET
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.HtmlContent
import unfiltered.scalate.Scalate
import edu.washington.cs.knowitall.nlpweb.tool.SrlIntent

object NlpWeb extends App with BasePage {
  val tools = Iterable(
    StemmerIntent,
    TokenizerIntent,
    PostaggerIntent,
    ChunkerIntent,
    ParserIntent,
    SentencerIntent,
    ExtractorIntent,
    SrlIntent,
    ConstituencyParserIntent).map(intent => (intent.path, intent)).toMap

  case class Config(port: Int = 8080)

  val argumentParser = new OptionParser[Config]("nlpweb") {
    def options = Seq(
      intOpt("p", "port", "output file (otherwise stdout)") { (port: Int, config: Config) =>
        config.copy(port = port)
      })
  }

  argumentParser.parse(args, Config()) match {
    case Some(config) => run(config)
    case None =>
  }

  def run(config: Config) = {
    def first = Intent {
      case req @ GET(Path("/")) => HtmlContent ~> Scalate(req, "/templates/main.jade")
    }

    def logIntent = Intent {
      case req @ GET(Path(Seg("log" :: number :: Nil))) =>
        Database.find(number.toLong) match {
          case Some(entry) =>
            val params = entry.params.map(_.toTuple).toMap
            val (stats, result) = tools(entry.path).post(entry.tool, params)
            basicPage(req,
              name = "Log " + number,
              id = entry.id,
              info = "",
              text = params("text"),
              config = "",
              stats = stats,
              result = result)
          case None => errorPage(req, "Log entry not found: " + number)
        }
    }

    val intent = tools.values.map(_.intent).reduce(_ orElse _) orElse DotIntent.intent orElse logIntent orElse first

    val plan = new unfiltered.filter.Planify(intent)

    println("starting...")
    try {
      Http(config.port).context("/public") { ctx: ContextBuilder =>
        ctx.resources(this.getClass.getResource("/pub"))
      }.filter(plan).run()
    } catch {
      case e: java.net.BindException => println("Address already in use: " + config.port); System.exit(1)
    }
  }
}

