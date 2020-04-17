package temple

import java.nio.file.{Files, Paths}

import temple.builder.project.Project
import temple.detail.QuestionAsker
import temple.generate.FileSystem.File
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.FileUtils

/**
  * Handles whether we're regenerating or not.
  */
object RegenerationFilter {

  private def confirmationString(omittedFileString: String): String =
    s"""The output directory already contains some files that are going to be generated.
       |This process will overwrite them.
       |These files will not be touched to preserve business logic:
       |$omittedFileString
       |Do you want to continue? (Y/N)""".stripMargin

  // Calculate which files shouldn't be regenerated
  private def filesToOmit(existingFiles: Seq[File]): Seq[File] =
    existingFiles.filter(file => file.filename == "setup.go" || file.filename == "datastore.go")

  // Get all the files under a directory, up to a max depth of 4 (the deepest Temple generates)
  // TODO: Should this be moved to FileUtils?
  private def filesUnderDirectory(directory: String): Seq[File] = {
    val directoryPath = Paths.get(directory)
    if (Files.exists(directoryPath)) {
      FileUtils.buildFileMap(directoryPath).keys.toSeq
    } else Seq()
  }

  // Check if any of the files we're generating already exist, i.e if we're regenerating something.
  private def isRegen(outputDir: String, existingFiles: Seq[File], project: Project): Boolean = {
    val generatedFiles = project.files.keys.toSet
    existingFiles.toSet.intersect(generatedFiles).nonEmpty
  }

  // Confirm with the user that they want to overwrite existing files
  private def shouldRegen(omittedFiles: Seq[File], questionAsker: QuestionAsker): Boolean = {
    val omittedFileString = mkCode.lines(omittedFiles.map(file => s"➤ ${file.toString}"))
    var answer            = ""
    while (!Seq("y", "n").contains(answer)) {
      answer = questionAsker.askQuestion(confirmationString(omittedFileString))
      answer = answer.toLowerCase.take(1)
    }
    answer == "y"
  }

  /**
    * Take a generated [[Project]], check if we are regenerating, confirm with the user,
    * and then filter the files from the project that shouldn't be regenerated.
    * @param outputDirectory The directory Temple will output the files to
    * @param project The generated project to filter
    * @param questionAsker The class to confirm with the user if they want to overwrite existing files
    * @return The project with any omitted files removed
    */
  def filter(
    outputDirectory: String,
    project: Project,
    questionAsker: QuestionAsker,
  ): Option[Project] = {
    val existingFiles = filesUnderDirectory(outputDirectory)
    // If we are regenerating, i.e some files already exist here
    if (isRegen(outputDirectory, existingFiles, project)) {
      val omittedFiles = filesToOmit(existingFiles)
      // Get the user to confirm whether we're regenerating
      Option.when(shouldRegen(omittedFiles, questionAsker)) {
        Project(project.files.filterNot {
          case file -> _ => omittedFiles.contains(file)
        })
      }
    } else {
      // If we're not regenerating anything, then carry on as normal
      Some(project)
    }
  }
}
