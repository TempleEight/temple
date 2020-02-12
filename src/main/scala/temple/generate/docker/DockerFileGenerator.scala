package temple.generate.docker

import temple.generate.docker.ast.DockerfileRoot

object DockerFileGenerator {

  def generate(dockerfileRoot: DockerfileRoot): String =
    "Hello, Temple!"
}
