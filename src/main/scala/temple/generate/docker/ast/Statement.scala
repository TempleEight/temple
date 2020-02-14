package temple.generate.docker.ast

sealed trait Statement

object Statement {
  case class From(image: String, tag: Option[String])     extends Statement
  case class Run(executable: String, params: Seq[String]) extends Statement
  case class Cmd(executable: String, params: Seq[String]) extends Statement
}
