package temple.utils

import java.io.IOException
import java.nio.charset.Charset
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import temple.builder.project.Project
import temple.generate.FileSystem
import temple.generate.FileSystem.File

import scala.io.{Source, StdIn}

/** Helper functions useful for manipulating files */
object FileUtils {

  def outputProject(directory: String, project: Project): Unit = {
    FileUtils.createDirectory(directory)
    project.files.foreach {
      case (file, contents) =>
        val subfolder = s"$directory/${file.folder}"
        FileUtils.createDirectory(subfolder)
        FileUtils.writeToFile(
          s"$subfolder/${file.filename}",
          contents,
        )
    }
  }

  def createDirectory(directory: String): Unit =
    Files.createDirectories(Paths.get(directory))

  /** Write a string to file */
  def writeToFile(filename: String, s: String): Unit = {
    val path   = Paths.get(filename)
    val writer = Files.newBufferedWriter(path, Charset.forName("UTF-8"))
    try writer.write(s)
    finally writer.close()
  }

  /** Read a string from a file */
  def readFile(filename: String): String = {
    val path = Paths.get(filename)
    Files.readString(path)
  }

  /** Read a string from standard input */
  def readStdIn(): String = Iterator.continually(StdIn.readLine).takeWhile(_ != null).mkString("\n")

  /** Read a string from a file in the resources folder */
  def readResources(filename: String): String =
    Source.fromResource(filename).mkString

  /** Read a byte array from a file */
  def readBinaryFile(filename: String): Array[Byte] = {
    val path = Paths.get(filename)
    Files.readAllBytes(path)
  }

  /**
    * Build a map of files present in the provided filepath
    * @param path The root path to explore
    * @return A map of files, relative to the provided path
    */
  def buildFileMap(path: Path): FileSystem.Files = buildFileMap(path, path)

  /**
    * Build a sequence of filenames of files present in the provided filepath
    * @param path The root path to explore
    * @return A sequence of filenames, relative to the provided path
    */
  def buildFilenameSeq(path: Path): Seq[File] = buildFilenameMap(path, path)

  /**
    * Build a map of files present in the provided filepath
    * @param path The root path to explore
    * @param subdirectory The subdirectory to restrict the search to
    * @return A map of files, relative to the provided path
    */
  def buildFileMap(path: Path, subdirectory: String): FileSystem.Files = buildFileMap(path.resolve(subdirectory), path)

  /**
    * Build a map of files present in the provided filepath
    * @param path The root path to explore
    * @param cwd The location to give paths relative to
    * @return A map of files, relative to the provided path
    */
  private def buildFileMap(path: Path, cwd: Path): FileSystem.Files = {
    var foundFiles: FileSystem.Files = Map.empty
    Files.walkFileTree(
      path,
      new FileVisitor[Path] {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
          FileVisitResult.CONTINUE

        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          foundFiles += File(
            Option(cwd.relativize(file).getParent).map(_.toString).getOrElse(""),
            file.getFileName.toString,
          ) -> FileUtils.readFile(
            file.toString,
          )
          FileVisitResult.CONTINUE
        }

        override def visitFileFailed(file: Path, exc: IOException): FileVisitResult =
          throw exc

        override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult =
          FileVisitResult.CONTINUE
      },
    )
    foundFiles
  }

  /**
    * Build a sequence of filenames of files present in the provided filepath
    * @param path The root path to explore
    * @param cwd The location to give paths relative to
    * @return A sequence of filenames, relative to the provided path
    */
  private def buildFilenameMap(path: Path, cwd: Path): Seq[File] = {
    var foundFiles: Seq[File] = Seq.empty
    Files.walkFileTree(
      path,
      new FileVisitor[Path] {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
          FileVisitResult.CONTINUE

        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          foundFiles = foundFiles :+ File(
              Option(cwd.relativize(file).getParent).map(_.toString).getOrElse(""),
              file.getFileName.toString,
            )
          FileVisitResult.CONTINUE
        }

        override def visitFileFailed(file: Path, exc: IOException): FileVisitResult =
          throw exc

        override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult =
          FileVisitResult.CONTINUE
      },
    )
    foundFiles
  }
}
