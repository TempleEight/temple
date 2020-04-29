package temple

import org.rogach.scallop.{ScallopConf, ScallopOption, Subcommand}

import scala.collection.{Seq => CSeq}

/** TempleConfig produces the application configuration from command line arguments */
class TempleConfig(arguments: CSeq[String]) extends ScallopConf(arguments) {
  errorMessageHandler = { error =>
    throw new IllegalArgumentException(error)
  }

  version(s"temple ${BuildInfo.version} (c) 2020 TempleEight")
  banner("Usage:\ttemple [OPTIONS] SUBCOMMAND")
  footer("Run 'temple SUBCOMMAND --help' for more information on a command.")

  // Simplify output at the top level
  shortSubcommandsHelp(true)

  object Generate extends Subcommand("generate") {

    val filename: ScallopOption[String] = trailArg[String]("filename", "Templefile to generate from")

    val outputDirectory: ScallopOption[String] =
      opt[String]("output", 'o', "Output directory to place generated files")
  }
  addSubcommand(Generate)

  object Validate extends Subcommand("validate") {

    val filename: ScallopOption[String] = trailArg[String]("filename", "Templefile to validate")
  }
  addSubcommand(Validate)

  object Test extends Subcommand("test") {
    val filename: ScallopOption[String] = trailArg[String]("filename", "Templefile to test against")

    val testOnly: ScallopOption[Boolean] =
      opt[Boolean]("testOnly", 't', "Do not spin up or shutdown any infrastructure as part of the testing")

    val generatedDirectory: ScallopOption[String] =
      opt[String]("dir", 'd', "Root directory where files have been generated")
  }
  addSubcommand(Test)

  verify()
}

object TempleConfig {
  class UnhandledArgumentException extends RuntimeException
}
