package temple.builder.project

import temple.utils.FileUtils.{File, FileContent}

case class Project(files: Map[File, FileContent])
