package temple.builder

import temple.generate.docker.ast.DockerfileRoot
import temple.generate.docker.ast.Statement._

object DockerfileBuilderTestData {

  val sampleServiceDockerfile: DockerfileRoot = DockerfileRoot(
    From("golang", Some("1.13.7-alpine")),
    Seq(
      WorkDir("/sampleservice"),
      Copy("go.mod", "./"),
      Run("go", Seq("mod", "download")),
      Copy(".", "."),
      Copy("config.json", "/etc/sampleservice-service/"),
      Run("go", Seq("build", "-o", "sampleservice")),
      Entrypoint("./sampleservice", Seq()),
      Expose(80),
    ),
  )

  val sampleComplexServiceDockerfile: DockerfileRoot = DockerfileRoot(
    From("golang", Some("1.13.7-alpine")),
    Seq(
      WorkDir("/complexservice"),
      Copy("go.mod", "./"),
      Run("go", Seq("mod", "download")),
      Copy(".", "."),
      Copy("config.json", "/etc/complexservice-service/"),
      Run("go", Seq("build", "-o", "complexservice")),
      Entrypoint("./complexservice", Seq()),
      Expose(80),
    ),
  )

  val sampleAuthServiceDockerComposeDockerfile: DockerfileRoot = DockerfileRoot(
    From("golang", Some("1.13.7-alpine")),
    Seq(
      WorkDir("/auth"),
      Copy("go.mod", "./"),
      Run("go", Seq("mod", "download")),
      Copy(".", "."),
      Copy("config.json", "/etc/auth-service/"),
      Run("go", Seq("build", "-o", "auth")),
      Run("apk", Seq("add", "curl")),
      Run("chmod", Seq("+x", "wait-for-kong.sh")),
      Cmd("./wait-for-kong.sh", Seq("kong:8001", "--", "./auth")),
      Expose(80),
    ),
  )

  val sampleAuthServiceKubernetesDockerfile: DockerfileRoot = DockerfileRoot(
    From("golang", Some("1.13.7-alpine")),
    Seq(
      WorkDir("/auth"),
      Copy("go.mod", "./"),
      Run("go", Seq("mod", "download")),
      Copy(".", "."),
      Copy("config.json", "/etc/auth-service/"),
      Run("go", Seq("build", "-o", "auth")),
      Entrypoint("./auth", Seq()),
      Expose(80),
    ),
  )
}
