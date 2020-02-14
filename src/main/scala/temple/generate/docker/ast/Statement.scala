package temple.generate.docker.ast

sealed trait Statement

object Statement {
  case class From(image: String, tag: Option[String])         extends Statement
  case class RunCmd(command: String)                          extends Statement
  case class RunExec(executable: String, params: Seq[String]) extends Statement
}
