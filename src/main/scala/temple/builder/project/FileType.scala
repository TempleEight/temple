package temple.builder.project

trait FileType

object FileType {

  case object SQL extends FileType {
    override def toString: String = "sql"
  }
}
