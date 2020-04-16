package temple

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path}

import temple.generate.FileSystem
import temple.generate.FileSystem.File
import temple.utils.FileUtils

object TestUtils {

  /**
    * Build a map of files present in the provided filepath
    * @param path The root path to explore
    * @return A map of files, relative to the provided path
    */
  def buildFileMap(path: Path): FileSystem.Files = buildFileMap(path, path)

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
}
