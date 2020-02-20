package temple.generate.docker.ast

case class DockerfileRoot(from: Statement.From, statements: Seq[Statement])
