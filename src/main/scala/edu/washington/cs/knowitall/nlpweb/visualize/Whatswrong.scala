package edu.washington.cs.knowitall.nlpweb.visualize

import java.awt.image.BufferedImage
import java.io.File
import com.googlecode.whatswrong.{ NLPInstance, SingleSentenceRenderer, TokenProperty }
import edu.washington.cs.knowitall.tool.chunk.{ ChunkedToken, OpenNlpChunker }
import edu.washington.cs.knowitall.tool.postag.PostaggedToken
import edu.washington.cs.knowitall.tool.tokenize.Token
import javax.imageio.ImageIO
import edu.washington.cs.knowitall.tool.parse.graph.DependencyGraph
import edu.washington.cs.knowitall.tool.parse.graph.DependencyNode
import edu.washington.cs.knowitall.tool.parse.MaltParser
import java.io.ByteArrayOutputStream
import java.util.prefs.Base64
import org.apache.commons.codec.binary.Base64OutputStream
import edu.washington.cs.knowitall.common.Resource
import edu.washington.cs.knowitall.tool.srl.Frame
import edu.washington.cs.knowitall.nlpweb.tool.SrlIntent.SrlPackage
import java.awt.Color

object Whatswrong {
  final val MAX_WIDTH = 2000
  final val MAX_HEIGHT = 200

  val renderer = new SingleSentenceRenderer()
  renderer.setEdgeTypeColor("amod", Color.pink)
  renderer.setEdgeTypeColor("prt", Color.pink)
  renderer.setEdgeTypeColor("advmod", Color.pink)
  renderer.setEdgeTypeColor("neg", Color.red)
  renderer.setEdgeTypeColor("det", Color.gray)
  renderer.setEdgeTypeColor("prep", Color.blue)
  renderer.setEdgeTypeColor("pobj", Color.blue)
  renderer.setEdgeTypeColor("argument", Color.blue)
  renderer.setEdgeTypeColor("relation", Color.red)
  renderer.setEdgeTypeColor("empty", Color.white)

  case class Base64String(string: String)

  implicit def tokenizeToken: WhatswrongTokenizer[Token] = new WhatswrongTokenizer[Token] {
    def tokenize(source: Token, target: com.googlecode.whatswrong.Token) = {
      target.addProperty(new TokenProperty("text", 0), source.string)
    }
  }
  implicit def tokenizePostag(implicit tokenizer: WhatswrongTokenizer[Token]): WhatswrongTokenizer[PostaggedToken] = new WhatswrongTokenizer[PostaggedToken] {
    def tokenize(source: PostaggedToken, target: com.googlecode.whatswrong.Token) {
      tokenizer.tokenize(source, target)
      target.addProperty(new TokenProperty("postag", 1), source.postag)
    }
  }
  implicit def tokenizeChunk(implicit tokenizer: WhatswrongTokenizer[PostaggedToken]): WhatswrongTokenizer[ChunkedToken] = new WhatswrongTokenizer[ChunkedToken] {
    def tokenize(source: ChunkedToken, target: com.googlecode.whatswrong.Token) {
      tokenizer.tokenize(source, target)
      target.addProperty(new TokenProperty("chunk", 2), source.chunk)
    }
  }
  implicit def tokenizeNode(implicit tokenizer: WhatswrongTokenizer[PostaggedToken]): WhatswrongTokenizer[DependencyNode] = new WhatswrongTokenizer[DependencyNode] {
    def tokenize(source: DependencyNode, target: com.googlecode.whatswrong.Token) {
      tokenizer.tokenize(source, target)
    }
  }
  def seq2Instance[A](seq: Seq[A])(implicit tokenizer: WhatswrongTokenizer[A]) = {
    val inst = new NLPInstance()
    for (token <- seq) {
      val tok = inst.addToken()
      tokenizer.tokenize(token, tok)
    }

    inst
  }
  def graph2Instance(graph: DependencyGraph) = {
    // get nodes
    val inst = seq2Instance[DependencyNode](graph.nodes.toSeq)

    // add edges
    for (edge <- graph.dependencies) {
      val source = edge.source.indices.head
      val dest = edge.dest.indices.head
      inst.addDependency(source, dest, edge.label, edge.label)
    }

    inst
  }

  def render(inst: NLPInstance) = {
    val bi = new BufferedImage(Whatswrong.MAX_WIDTH, Whatswrong.MAX_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    val graphic: java.awt.Graphics2D = bi.createGraphics()
    val dimensions = renderer.synchronized {
      renderer.render(inst, graphic)
    }

    bi.getSubimage(0, 0, dimensions.width, dimensions.height)
  }
  def writeSeq2Graphic[A](implicit tokenizer: WhatswrongTokenizer[A]) = new CanWrite[Seq[A], BufferedImage] {
    override def write(tokens: Seq[A]): BufferedImage = {
      val inst = seq2Instance[A](tokens)
      render(inst)
    }
  }
  implicit def writeChunkSeq2Graphic[Token] = writeSeq2Graphic(tokenizeChunk)
  implicit def writePostagSeq2Graphic[PostaggedToken] = writeSeq2Graphic(tokenizePostag)
  implicit def writeTokenSeq2Graphic[ChunkedToken] = writeSeq2Graphic(tokenizeToken)

  implicit def writeGraph2Graphic = new CanWrite[DependencyGraph, BufferedImage] {
    val renderer = new SingleSentenceRenderer()
    override def write(graph: DependencyGraph): BufferedImage = {
      val inst = graph2Instance(graph)
      render(inst)
    }
  }

  implicit def writeFrames2Graphic = new CanWrite[(DependencyGraph, Frame), BufferedImage] {
    val renderer = new SingleSentenceRenderer()
    override def write(srl: (DependencyGraph, Frame)): BufferedImage = {
      val (graph, frame) = srl
      val inst = graph2Instance(graph)
      var indices = Set.empty[Int]
      inst.addSpan(frame.relation.node.indices.head, frame.relation.node.indices.head, frame.relation.toString, "relation")
      indices += frame.relation.node.indices.head
      for (argument <- frame.arguments) {
        inst.addSpan(argument.node.indices.head, argument.node.indices.head, argument.role.label, "argument")
        indices += argument.node.indices.head
      }
      render(inst)
    }
  }

  implicit def writeGraphic2Base64[A](implicit writer: CanWrite[A, BufferedImage]) = new CanWrite[A, Base64String] {
    override def write(a: A) = {
      val bi = writer.write(a)
      Resource.using(new ByteArrayOutputStream()) { os =>
        Resource.using(new Base64OutputStream(os)) { b64 =>
          ImageIO.write(bi, "png", b64);
          Base64String(os.toString("UTF-8"))
        }
      }
    }
  }

  trait WhatswrongTokenizer[A] {
    def tokenize(source: A, dest: com.googlecode.whatswrong.Token)
  }

  trait CanRead[A, B] {
    def read(input: A): Either[String, B]
  }
  trait CanWrite[A, B] {
    def write(input: A): B
  }
  trait Format[A, B] extends CanRead[A, B] with CanWrite[A, B]

}
