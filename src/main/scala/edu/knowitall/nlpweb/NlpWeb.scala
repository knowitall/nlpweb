package edu.knowitall.nlpweb

import edu.knowitall.nlpweb.persist.Database
import edu.knowitall.nlpweb.tool.ChunkerIntent
import edu.knowitall.nlpweb.tool.ConstituencyParserIntent
import edu.knowitall.nlpweb.tool.ExtractorIntent
import edu.knowitall.nlpweb.tool.ParserIntent
import edu.knowitall.nlpweb.tool.PostaggerIntent
import edu.knowitall.nlpweb.tool.SentencerIntent
import edu.knowitall.nlpweb.tool.StemmerIntent
import edu.knowitall.nlpweb.tool.TokenizerIntent
import scopt.immutable.OptionParser
import unfiltered.filter.Intent
import unfiltered.jetty.ContextBuilder
import unfiltered.jetty.Http
import unfiltered.request.GET
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.HtmlContent
import unfiltered.scalate.Scalate
import edu.knowitall.nlpweb.tool.SrlIntent
import java.net.URL
import java.io.File
import java.net.MalformedURLException
import edu.knowitall.common.Resource
import scala.io.Source
import org.slf4j.LoggerFactory

object NlpWeb extends App with BasePage {
  val logger = LoggerFactory.getLogger(NlpWeb.getClass)
  val tools: Map[String, ToolIntent[_]] = Iterable[ToolIntent[_]](
    StemmerIntent,
    TokenizerIntent,
    PostaggerIntent,
    ChunkerIntent,
    ParserIntent,
    SentencerIntent,
    ExtractorIntent,
    SrlIntent,
    ConstituencyParserIntent).map(intent => intent.path -> intent).toMap

  case class Config(port: Int = 8080,
    remotesUrl: Option[URL] = None) {

    def loadRemotes(): Map[String, URL] = {
      remotesUrl match {
        case None => Map.empty
        case Some(remotesUrl) =>
          Resource.using(remotesUrl.openStream) { stream =>
            Source.fromInputStream(stream).getLines.filter(!_.trim.isEmpty).map { line =>
              val (name, url) = line.split("\t") match {
                case Array(name, url) => (name, new URL(url))
                case _ => throw new MatchError("Could not parse line: " + line)
              }
              logger.info("Remote configuration: '" + name + "' at '" + url + "'")
              (name, url)
            }.toMap
          }
      }
    }
  }

  val argumentParser = new OptionParser[Config]("nlpweb") {
    def options = Seq(
      intOpt("p", "port", "output file (otherwise stdout)") { (port: Int, config: Config) =>
        config.copy(port = port)
      },
      opt("r", "remotes", "remotes file") { (address: String, config: Config) =>
        val url =
          scala.util.control.Exception.catching(classOf[MalformedURLException]) opt new URL(address) match {
            case Some(url) => url
            case None => new File(address).toURI.toURL
          }
        config.copy(remotesUrl = Some(url))
      })
  }

  argumentParser.parse(args, Config()) match {
    case Some(config) => run(config)
    case None =>
  }

  var remotes: Map[String, URL] = _

  def run(config: Config) = {
    def first = Intent {
      case req @ GET(Path("/")) => HtmlContent ~> Scalate(req, "/templates/main.jade")
    }

    remotes = config.loadRemotes()

    def logIntent = Intent {
      case req @ GET(Path(Seg("log" :: number :: Nil))) =>
        logger.info("Loading log entry: " + number)
        unfiltered.response.ResponseString("disabled")
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

