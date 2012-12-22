package edu.washington.cs.knowitall.nlpweb.persist

import org.orbroker._
import org.orbroker.config._
import org.orbroker.conv.BigDecimalConv
import java.io.File

object Database {
  val jdbcUrl = "jdbc:derby:nlpdb"
  val config = new BrokerConfig(jdbcUrl)
  FileSystemRegistrant(new File("sql")).register(config)
  config.verify(Tokens.idSet)
  val broker = Broker(config)

  def logs = {
    broker.readOnly() { session =>
      session.selectAll(Tokens.selectLogEntry)
    }
  }

  def find(id: Long) = {
    broker.readOnly() { session =>
      session.selectOne(Tokens.selectLogEntryById, "id" -> id)
    }
  }
}

object Tokens extends TokenSet(true) {
  val selectLogEntry = Token('selectLogEntry, LogEntryExtractor)
  val selectLogEntryById = Token('selectLogEntryById, LogEntryExtractor)
  val insertLogEntry = Token[Long]('insertLogEntry, BigDecimalConv)
  val insertParam = Token[Long]('insertParam, BigDecimalConv)
}