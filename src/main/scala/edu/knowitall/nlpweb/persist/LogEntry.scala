package edu.knowitall.nlpweb.persist

import org.orbroker.Row
import org.orbroker.RowExtractor
import org.orbroker.Join
import org.orbroker.JoinExtractor

case class LogEntry(val id: Option[Long], val path: String, tool: String, val params: IndexedSeq[Param]) {
  def persist() = {
    Database.broker.transaction() { txn =>
      val newId = txn.executeForKey[java.math.BigDecimal](Tokens.insertLogEntry, "logentry" -> this).map(_.longValue)
      for (param <- params) {
        txn.execute(Tokens.insertParam, "id" -> newId.get, "param" -> param)
      }
      this.copy(id = newId)
    }
  }
}

case class Param(key: String, value: String) {
  def toTuple = (key, value)
}

object ParamExtractor extends RowExtractor[Param] {
  def extract(row: Row) = {
    new Param(
      row.string("k").get,
      row.string("v").get)
  }
}

object LogEntryExtractor extends JoinExtractor[LogEntry] {
  val key = Set("LOGENTRY_ID")
  def extract(row: Row, join: Join) = {
    new LogEntry(
      row.decimal("LOGENTRY_ID").map(_.longValue),
      row.string("PATH").get,
      row.string("TOOL").get,
      join.extractSeq(ParamExtractor))
  }
}
