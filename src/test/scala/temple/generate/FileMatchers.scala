package temple.generate

import org.scalactic.source.Position
import org.scalatest.Matchers
import temple.builder.project.Project
import temple.generate.FileMatchers.WrappedFiles
import temple.generate.FileSystem.Files
import temple.utils.StringUtils.indent

import scala.collection.immutable.SortedMap

trait FileMatchers extends Matchers {

  final protected def filesShouldMatch(actual: Files, expected: Files)(implicit here: Position): Unit =
    WrappedFiles(actual) shouldBe WrappedFiles(expected)

  final protected def projectFilesShouldMatch(actual: Project, expected: Files)(implicit here: Position): Unit =
    WrappedFiles(actual.files) shouldBe WrappedFiles(expected)
}

object FileMatchers {

  private case class WrappedFiles(files: Files) {

    override def toString: String =
      files.to(SortedMap).map { case (file, content) => s"$file:\n${indent(content)}" }.mkString("\n")
  }
}
