package edu.knowitall.nlpweb

import edu.knowitall.nlpweb.persist.Database
import edu.knowitall.nlpweb.tool.ChunkerIntent
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

  lazy val tools: Map[String, ToolIntent[_]] = Iterable[ToolIntent[_]](
    StemmerIntent,
    TokenizerIntent,
    PostaggerIntent,
    ChunkerIntent,
    ParserIntent,
    SentencerIntent,
    ExtractorIntent,
    SrlIntent).map(intent => intent.path -> intent).toMap

  case class Config(port: Int = 8080,
    remoteUrl: Option[URL] = None) {

    def loadRemotes(): Option[RemoteServer] = {
      remoteUrl match {
        case None => None
        case Some(serverUrl) =>
          val indexUrl = new URL(serverUrl, "plaintext")
          val paths = Resource.using(indexUrl.openStream) { stream =>
            Source.fromInputStream(stream).getLines.map { path => 
              val trimmed = path.trim
              if (trimmed endsWith "/") trimmed.dropRight(1)
              else trimmed
            }.filter(!_.isEmpty).toSet
          }
          
          Some(RemoteServer(serverUrl, paths))
      }
    }
  }

  val argumentParser = new OptionParser[Config]("nlpweb") {
    def options = Seq(
      intOpt("p", "port", "output file (otherwise stdout)") { (port: Int, config: Config) =>
        config.copy(port = port)
      },
      opt("r", "remote", "nlptools server") { (address: String, config: Config) =>
        val url = new URL(address)
        config.copy(remoteUrl=Some(url))
      })
  }

  argumentParser.parse(args, Config()) match {
    case Some(config) => run(config)
    case None =>
  }

  case class RemoteServer(url: URL, paths: Set[String]) {
    def contains(basePath: String, name: String) = paths contains ("/" + basePath + "/" + name)
    def toolUrl(path: String) = new URL(url, path)
  }

  final var remote: Option[RemoteServer] = None

  def run(config: Config) = {
    def first = Intent {
      case req @ GET(Path("/")) => HtmlContent ~> Scalate(req, "/templates/main.jade")
    }

    remote = config.loadRemotes()

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

    val plan = unfiltered.filter.Planify(intent)

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
