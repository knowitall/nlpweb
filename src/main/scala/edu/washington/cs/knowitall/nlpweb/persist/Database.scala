package edu.washington.cs.knowitall.nlpweb.persist

import org.orbroker._
import org.orbroker.config._
import org.orbroker.conv.BigDecimalConv
import java.io.File
import java.sql.DriverManager
import edu.washington.cs.knowitall.common.Resource

object Database {
  val jdbcUrl = "jdbc:derby:nlpdb"
  val config = new BrokerConfig(jdbcUrl)
  ClasspathRegistrant(Map(
    'insertLogEntry -> "/sql/insertLogEntry.sql",
    'insertParam -> "/sql/insertParam.sql",
    'selectLogEntry -> "/sql/selectLogEntry.sql",
    'selectLogEntryById -> "/sql/selectLogEntryById.sql",
    'selectParam -> "/sql/selectParam.sql")).register(config)
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

object CreateDatabase extends App {
  def drop(conn: java.sql.Connection)(tableName: String) = {
    try {
      val stmt = conn.createStatement()
      stmt.executeUpdate("DROP TABLE " + tableName)
      conn.commit()
    } catch { case e => println("Error dropping table: " + tableName) }
  }

  val database = if (args.length > 0) args(0) else "nlpdb"
  println("opening connection: " + database)
  Resource.using(DriverManager.getConnection("jdbc:derby:" + database + ";create=true")) { conn =>
    println("Dropping old tables...")
    drop(conn)("LogEntry")
    drop(conn)("Param")

    println("Creating new tables...")
    val stmt = conn.createStatement()
    stmt.executeUpdate("CREATE TABLE LogEntry(LOGENTRY_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), PATH VARCHAR(64) NOT NULL, TOOL VARCHAR(64) NOT NULL)")
    stmt.executeUpdate("CREATE TABLE Param(LOGENTRY_ID INT NOT NULL, K VARCHAR(64) NOT NULL, V CLOB)")
    conn.commit()
  }

  println("done.")
}

object Tokens extends TokenSet(true) {
  val selectLogEntry = Token('selectLogEntry, LogEntryExtractor)
  val selectLogEntryById = Token('selectLogEntryById, LogEntryExtractor)
  val insertLogEntry = Token[Long]('insertLogEntry, BigDecimalConv)
  val insertParam = Token[Long]('insertParam, BigDecimalConv)
}