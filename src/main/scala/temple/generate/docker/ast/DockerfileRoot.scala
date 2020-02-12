package temple.generate.docker.ast

case class DockerfileRoot(from: Statement.From.type, statements: Seq[Statement])
