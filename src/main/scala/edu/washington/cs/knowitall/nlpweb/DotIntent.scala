package edu.washington.cs.knowitall.nlpweb

import java.io.PrintWriter
import java.net.URLConnection
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import unfiltered.filter.Intent
import unfiltered.request.GET
import unfiltered.request.POST
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.ComposeResponse
import unfiltered.response.ContentType
import unfiltered.response.ResponseBytes
import java.net.URLDecoder

object DotIntent extends BasePage {
  val logger = LoggerFactory.getLogger(this.getClass)
  lazy val dotFormats = {
    val target = "Use one of:"
    try {
      val p = Runtime.getRuntime().exec("dot -Tasdf")
      p.getOutputStream().close

      val response = IOUtils.toString(p.getErrorStream)

      if (p.exitValue == 127) {
        // format not found
        List.empty
      } else {
        val index = response.indexOf(target)
        if (index == -1)
          // output not as expected
          List()
        else {
          // we found the formats
          response.substring(index + target.length).trim.toLowerCase.split("\\s+").toList
        }
      }
    } catch {
      case _ => List.empty
    }
  }

  def intent = Intent {
    case req @ GET(Path("/dotter/")) =>
      basicPage(req,
        name = "Dotter",
        text = "",
        info = "",
        config = "",
        stats = "",
        result = "")

    case req @ GET(Path(Seg("dotter" :: text :: Nil))) =>
      basicPage(req,
        name = "Dotter",
        info = "",
        text = text,
        config = "",
        stats = "",
        result = "")

    case req @ POST(Path(Seg("dotter" :: xs))) =>
      val code = req.parameterValues("text").headOption.get
        .replaceAll("\\n", " ")
        .replaceAll("""\s+""", " ")
        .replaceAll("\"", """%22""")
      basicPage(req,
        name = "Dotter",
        text = code,
        result = "<img src=\"/dot/png/" + code + "\">")

    case req @ GET(Path(Seg("dot" :: format :: xs))) if !(dotFormats contains format) =>
      errorPage(req, "Format not supported: " + format + ".\n" + "Supported formats by dot: " + dotFormats.mkString("\n"))

    case req @ GET(Path(Seg("dot" :: format :: Nil))) =>
      errorPage(req, "You must specify pass a DOT graph as a parameter.")

    case req @ GET(Path(Seg("dot" :: format :: code :: Nil))) =>
      val decoded = URLDecoder.decode(code, "UTF-8")
      if (!(dotFormats contains format)) {
        errorPage(req, "Format not supported: " + format)
      } else {
        val contentType = guessContentType(format)
        logger.debug("Sending to dot: " + contentType + ": " + decoded)
        val p = Runtime.getRuntime().exec("dot -T" + format)
        val pw = new PrintWriter(p.getOutputStream)
        try {
          pw.write(decoded)
        } finally {
          pw.close()
        }

        // copy the error (if any)
        IOUtils.copy(p.getErrorStream, System.err)
        val bytes = IOUtils.toByteArray(p.getInputStream)
        new ComposeResponse(ContentType(contentType) ~> ResponseBytes(bytes))
      }
  }

  private def guessContentType(ext: String) =
    if (ext == "svg") "text/html"
    else URLConnection.guessContentTypeFromName("." + ext)
}