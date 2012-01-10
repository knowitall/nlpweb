package edu.washington.cs.knowitall.nlpweb

import org.scalatra._
import scalate.ScalateSupport
import java.net.URL

import scala.collection.JavaConversions._

import org.apache.commons.io.IOUtils

import java.net.URLEncoder
import java.net.URLConnection
import java.io.PrintWriter

object RootFilter {
  lazy val dotFormats = {
    val target = "Use one of:"
    try {
      val p = Runtime.getRuntime().exec("dot -Tasdf")
      p.getOutputStream().close
      
      val response = IOUtils.toString(p.getErrorStream)
      
      if (p.exitValue == 127) {
        // format not found
        None
      }
      else {
        val index = response.indexOf(target)
        if (index == -1) 
          // output not as expected
          Some(List())
        else {
          // we found the formats
          Some(response.substring(index + target.length).trim.toLowerCase.split("\\s+").toList)
        }
      }
    }
    catch {
      case _ => None
    }
  }
}

class RootFilter extends BaseFilter {
  import RootFilter._

  /* Root */
  get("/") {
    contentType = "text/html"
    templateEngine.layout(buildTemplatePath("main"))
  }

  get("/dotter/") {
    basicPage(
      name="Dotter", 
      text="", 
      info="", 
      config="", 
      stats="",
      result="")
  }

  get("/dotter/:text") {
    basicPage(
      name="Dotter", 
      info="",
      text=params("text"), 
      config="", 
      stats="", 
      result="")
  }

  post("/dotter/*") {
    val code = params("text")
      .replaceAll("\\n", " ")
      .replaceAll("""\s+""", " ")
      .replaceAll("\"", """%22""")
    basicPage(
      name = "Dotter", 
      text = params("text"), 
      result = "<img src=\"" + servletContext.getContextPath + "/dot/png/"+code+"\">")
  }

  get("/dot/*") {
    indexPage(dotFormats.map(_.map("dot/" + _ + "/")).getOrElse(List()))
  }

  get("/dot/:format/") {
    errorPage("You must specify pass a DOT graph as a parameter.")
  }

  get("/dot/:format/:code") {
    def guessContentType(ext: String) =
      if (ext == "svg") "text/html"
      else URLConnection.guessContentTypeFromName("." + ext)

    val format = params("format")
    val code = params("code")
    if (!dotFormats.map(_.contains(format)).getOrElse(true)) {
      errorPage("Format not supported: " + format)
    }
    else {
      contentType=guessContentType(format)
      val p = Runtime.getRuntime().exec("dot -T" + format)
      val pw = new PrintWriter(p.getOutputStream)
      try {
        pw.write(code)
      }
      finally {
        pw.close()
      }

      // copy the error (if any)
      // IOUtils.copy(p.getErrorStream, System.err)
      IOUtils.toByteArray(p.getInputStream)
    }
  }
}
