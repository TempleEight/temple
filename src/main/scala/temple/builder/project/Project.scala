package temple.builder.project

import temple.builder.project.Project.Filename

case class Project(databaseCreationScripts: Map[Filename, String])

object Project {
  type Filename = String
}
