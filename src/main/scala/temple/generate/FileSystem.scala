package temple.generate

object FileSystem {
  type Files       = Map[File, FileContent]
  type FileContent = String

  case class File(folder: String, filename: String) extends Ordered[File] {
    import scala.math.Ordered.orderingToOrdered

    def compare(that: File): Int = (this.folder, this.filename) compare (that.folder, that.filename)

    override def toString: FileContent = s"$folder/$filename"
  }
}
