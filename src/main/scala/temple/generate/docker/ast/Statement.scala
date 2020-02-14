package temple.generate.docker.ast

sealed trait Statement

object Statement {
  case class From(image: String, tag: Option[String])     extends Statement
  case class Run(executable: String, params: Seq[String]) extends Statement
  case class Cmd(executable: String, params: Seq[String]) extends Statement

  case class Expose(port: Int) extends Statement {
    {
      val MAX_PORT = 65535
      if (!(0 to MAX_PORT contains port)) throw new IllegalArgumentException("EXPOSE requires a valid port")
    }
  }
}
