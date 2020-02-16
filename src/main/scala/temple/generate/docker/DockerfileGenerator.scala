package temple.generate.docker

import io.circe.Printer
import io.circe.syntax._
import temple.generate.docker.ast.Statement._
import temple.generate.docker.ast.{DockerfileRoot, Statement}
import temple.generate.utils.CodeTerm.mkCode

/** Generator object for building Dockerfiles from the Dockerfile AST objects */
object DockerfileGenerator {

  /** Given a sequence of strings, build them in to a string of form ["a", "b", "c"] */
  private def buildArrayString(strs: Seq[String]): String =
    strs.asJson.printWith(Printer(dropNullValues = false, indent = "", arrayCommaRight = " "))

  /** Given a [[temple.generate.docker.ast.Statement]], generate a valid string */
  private def generateStatement(statement: Statement): String =
    statement match {
      case From(image, tag)               => mkCode("FROM", image, tag.map(":" + _))
      case Run(executable, params)        => mkCode("RUN", buildArrayString(executable +: params))
      case Cmd(executable, params)        => mkCode("CMD", buildArrayString(executable +: params))
      case Expose(port)                   => mkCode("EXPOSE", port.toString)
      case Env(key, value)                => mkCode("ENV", key, value)
      case Add(src, dest)                 => mkCode("ADD", src, dest)
      case Copy(src, dest)                => mkCode("COPY", src, dest)
      case Entrypoint(executable, params) => mkCode("ENTRYPOINT", buildArrayString(executable +: params))
      case Volume(volume)                 => mkCode("VOLUME", volume)
      case WorkDir(dir)                   => mkCode("WORKDIR", dir)
    }

  /** Given a [[temple.generate.docker.ast.DockerfileRoot]] object, build a valid Dockerfile string */
  def generate(dockerfileRoot: DockerfileRoot): String = {
    //All valid Dockerfiles must begin with a FROM statement
    val statementStrings = generateStatement(dockerfileRoot.from) +: dockerfileRoot.statements.map(generateStatement)
    statementStrings.mkString("", "\n\n", "\n")
  }
}
