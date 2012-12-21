import edu.washington.cs.knowitall.nlpweb.persist.Database
import edu.washington.cs.knowitall.nlpweb.persist.Tokens

object Test extends App {
  val entries = Database.broker.readOnly() { session =>
    session.selectAll(Tokens.selectLogEntry)
  }

  entries foreach println
}