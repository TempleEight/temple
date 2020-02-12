package temple.generate

import temple.generate.docker.ast.DockerfileRoot
import temple.generate.docker.ast.Statement._

package object docker {

  object TestData {

    val basicDockerFileRoot: DockerfileRoot = DockerfileRoot(
      From("temple/base", Some("1.2.3")),
      Seq(
        Copy,
        Run,
        Run,
        Cmd,
      ),
    )

    val basicDockerFileString: String =
      """|FROM temple/base:1.2.3
        |
        |COPY
        |
        |RUN
        |
        |RUN
        |
        |CMD""".stripMargin
  }
}
