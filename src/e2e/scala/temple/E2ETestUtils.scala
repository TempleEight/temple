package temple

import java.io.IOException
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path}
import java.nio.file.attribute.BasicFileAttributes

import temple.generate.FileSystem
import temple.generate.FileSystem.File
import temple.utils.FileUtils

object E2ETestUtils {

  /**
    * Build a map of files present in the provided filepath
    * @param path The root path to explore
    * @return A map of files, relative to the provided path
    */
  def buildFileMap(path: Path): FileSystem.Files = {
    var foundFiles: FileSystem.Files = Map.empty
    Files.walkFileTree(
      path,
      new FileVisitor[Path] {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
          FileVisitResult.CONTINUE

        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          foundFiles += File(path.relativize(file).getParent.toString, file.getFileName.toString) -> FileUtils.readFile(
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
