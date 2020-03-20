package temple.generate

object FileSystem {
  type Files       = Map[File, FileContent]
  type FileContent = String
  case class File(folder: String, filename: String)
}
