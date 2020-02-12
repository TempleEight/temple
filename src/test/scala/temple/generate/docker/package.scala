package temple.generate

import temple.generate.docker.ast.DockerfileRoot
import temple.generate.docker.ast.Statement._

package object docker {

  object TestData {

    val basicDockerFileRoot: DockerfileRoot = DockerfileRoot(
      Seq(
        From,
        Copy,
        Run,
        Run,
        Cmd,
      ),
    )

    val basicDockerFileString: String =
      """|FROM
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
