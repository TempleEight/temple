package temple.generate.docker

import temple.generate.docker.ast.DockerfileRoot
import temple.generate.docker.ast.Statement._

object UnitTestData {

  val basicDockerfileRoot: DockerfileRoot = DockerfileRoot(
    From("temple/base", Some("1.2.3")),
    Seq(
      RunCmd("/bin/bash"),
      RunExec("/bin/python3", Seq("src/main.py", "--help")),
    ),
  )

  val basicDockerfileString: String =
    """|FROM temple/base:1.2.3
       |
       |RUN /bin/bash
       |
       |RUN ["/bin/python3", "src/main.py", "--help"]
       |""".stripMargin
}
