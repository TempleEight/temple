package temple

import temple.builder.project.Project
import temple.generate.FileSystem.File

object RegenerationFilterTestData {

  val simpleTestProjectWithConflict: Project = Project(
    Map(
      File("", "test-file.go") -> "test-contents",
      File("", "setup.go")     -> "setup.go contents",
    ),
  )
}
