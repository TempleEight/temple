package temple.generate

object FileSystem {
  type FileContent = String
  case class File(folder: String, filename: String)
}
