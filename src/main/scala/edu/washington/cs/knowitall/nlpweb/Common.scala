package edu.washington.cs.knowitall.nlpweb

object Common {
  def timed[R](block: =>R) = {
    val start = System.currentTimeMillis
    val result = block
    (System.currentTimeMillis - start, result)
  }
}
