package temple.generate.docker

import io.circe.Printer
import io.circe.syntax._
import temple.generate.docker.ast.Statement._
import temple.generate.docker.ast.{DockerfileRoot, Statement}

/** Generator object for building Dockerfiles from the Dockerfile AST objects */
object DockerFileGenerator {

  /** Given a sequence of strings, build them in to a string of form ["a", "b", "c"] */
  private def buildArrayString(strs: Seq[String]): String =
    strs.asJson.printWith(Printer(dropNullValues = false, indent = "", arrayCommaRight = " "))

  /** Given a [[temple.generate.docker.ast.Statement]], generate a valid string */
  private def generateStatement(statement: Statement): String =
    statement match {
      case From(image, tag) =>
        s"FROM $image" + (tag match {
          case Some(tagString) => s":$tagString"
          case None            => ""
        })
      case RunCmd(command)             => s"RUN $command"
      case RunExec(executable, params) => s"RUN ${buildArrayString(executable +: params)}"
    }

  /** Given a [[temple.generate.docker.ast.DockerfileRoot]] object, build a valid Dockerfile string */
  def generate(dockerfileRoot: DockerfileRoot): String = {
    //All valid Dockerfiles must begin with a FROM statement
    val statementStrings = generateStatement(dockerfileRoot.from) +: dockerfileRoot.statements.map(generateStatement)
    statementStrings.mkString("", "\n\n", "\n")
  }
}
