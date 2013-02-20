package edu.washington.cs.knowitall.nlpweb

import java.awt.image.BufferedImage
import java.io.File

import edu.washington.cs.knowitall.nlpweb.visualize.Whatswrong.{CanWrite, writeGraph2Graphic}
import edu.washington.cs.knowitall.tool.parse.MaltParser
import edu.washington.cs.knowitall.tool.parse.graph.DependencyGraph
import javax.imageio.ImageIO

object Test extends App {
  val parser = new MaltParser()
  val graph = parser.dependencyGraph("Michael ran down the street and broke his leg.")
  val graphic = implicitly[CanWrite[DependencyGraph, BufferedImage]].write(graph)
  ImageIO.write(graphic, "PNG", new File("/homes/gws/schmmd/foo.png"));
}
