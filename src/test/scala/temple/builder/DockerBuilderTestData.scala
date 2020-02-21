package temple.builder

import temple.generate.docker.ast.DockerfileRoot
import temple.generate.docker.ast.Statement.{Copy, Entrypoint, Expose, From, Run, WorkDir}

object DockerBuilderTestData {

  val sampleServiceDockerfile: DockerfileRoot = DockerfileRoot(
    From("golang", Some("1.13.7-alpine")),
    Seq(
      WorkDir("/sampleservice"),
      Copy("go.mod go.sum", "./"),
      Run("go", Seq("mod", "download")),
      Copy(".", "."),
      Copy("config.json", "/etc/sampleservice-service"),
      Run("go", Seq("build", "-o", "sampleservice")),
      Entrypoint("./sampleservice", Seq()),
      Expose(80),
    ),
  )

  val sampleComplexServiceDockerfile: DockerfileRoot = DockerfileRoot(
    From("golang", Some("1.13.7-alpine")),
    Seq(
      WorkDir("/complexservice"),
      Copy("go.mod go.sum", "./"),
      Run("go", Seq("mod", "download")),
      Copy(".", "."),
      Copy("config.json", "/etc/complexservice-service"),
      Run("go", Seq("build", "-o", "complexservice")),
      Entrypoint("./complexservice", Seq()),
      Expose(80),
    ),
  )
}
