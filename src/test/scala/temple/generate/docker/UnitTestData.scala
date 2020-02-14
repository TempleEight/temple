package temple.generate.docker

import temple.generate.docker.ast.DockerfileRoot
import temple.generate.docker.ast.Statement._

object UnitTestData {

  val basicDockerfileRoot: DockerfileRoot = DockerfileRoot(
    From("temple/base", Some("1.2.3")),
    Seq(
      Run("/bin/python3", Seq("src/main.py", "--help")),
      Cmd("/bin/bash", Seq("/app/start.sh")),
      Expose(1234),
      Env("key", "value"),
      Add("src/*", "/app/"),
      Copy("bin/*", "/app/bin"),
      Entrypoint("/usr/bin/nginx", Seq("--help")),
    ),
  )

  val basicDockerfileString: String =
    """|FROM temple/base:1.2.3
       |
       |RUN ["/bin/python3", "src/main.py", "--help"]
       |
       |CMD ["/bin/bash", "/app/start.sh"]
       |
       |EXPOSE 1234
       |
       |ENV key value
       |
       |ADD src/* /app/
       |
       |COPY bin/* /app/bin
       |
       |ENTRYPOINT ["/usr/bin/nginx", "--help"]
       |""".stripMargin
}
