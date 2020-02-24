package temple

import org.rogach.scallop.{ScallopConf, ScallopOption, Subcommand}

import scala.collection.{Seq => CSeq}

/** TempleConfig produces the application configuration from command line arguments */
class TempleConfig(arguments: CSeq[String]) extends ScallopConf(arguments) {
  errorMessageHandler = { error =>
    throw new IllegalArgumentException(error)
  }

  version("temple " + BuildInfo.version + " (c) 2020 TempleEight")
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
  verify()
}

object TempleConfig {
  class UnhandledArgumentException extends RuntimeException
}
