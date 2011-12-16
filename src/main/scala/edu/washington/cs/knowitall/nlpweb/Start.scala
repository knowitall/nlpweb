/*
package edu.washington.cs.knowitall.nlpweb
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.bio.SocketConnector
import org.eclipse.jetty.webapp.WebAppContext

object Start {
  val DEFAULT_PORT = 8088
  
  def main(args: Array[String]) {
    val port = if (args.length > 0) args(0).toInt else DEFAULT_PORT
    
    val server = new Server()
    val connector = new SocketConnector()
    connector.setPort(8088)
    server.setConnectors(Array(connector))
    
    val context = new WebAppContext()
    context.setServer(server)
    context.setContextPath("/")
    context.setWar("src/main/webapp")
    server.setHandler(context)
    
    println("Starting embedded jetty server on port " + port + ".")
    server.start()
    
    System.in.read()
      
    server.stop()
    server.join()
  }

}
*/
