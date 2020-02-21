package temple.builder.project

import temple.builder.project.Project.File

case class Project(files: Map[File, String])

object Project {
  case class File(folder: String, filename: String)
}
