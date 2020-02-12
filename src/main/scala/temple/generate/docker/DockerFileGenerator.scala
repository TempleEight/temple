package temple.generate.docker

import temple.generate.docker.ast.Statement._
import temple.generate.docker.ast.{DockerfileRoot, Statement}

/** Generator object for building Dockerfiles from the Dockerfile AST objects */
object DockerFileGenerator {

  /** Given a [ast.Statement], generate a valid string */
  private def generateStatement(statement: Statement): String =
    statement match {
      case Env        => "ENV"
      case From       => "FROM"
      case Run        => "RUN"
      case Cmd        => "CMD"
      case Label      => "LABEL"
      case Expose     => "EXPOSE"
      case Add        => "ADD"
      case Copy       => "COPY"
      case EntryPoint => "ENTRYPOINT"
      case Volume     => "VOLUME"
      case User       => "USER"
      case WorkDir    => "WORKDIR"
      case Arg        => "ARG"
    }

  /** Given a [ast.DockerfileRoot] object, build a valid Dockerfile string */
  def generate(dockerfileRoot: DockerfileRoot): String = {
    //All valid Dockerfiles must begin with a FROM statement
    val statementStrings = generateStatement(dockerfileRoot.from) +: dockerfileRoot.statements.map(generateStatement)
    statementStrings.mkString("\n\n")
  }
}
