package temple

import org.rogach.scallop.{ScallopConf, ScallopOption, Subcommand}

/** TempleConfig produces the application configuration from command line arguments */
class TempleConfig(arguments: Seq[String]) extends ScallopConf(arguments) {
  version("temple 0.1 (c) 2020 TempleEight")
  banner("Usage:\ttemple [OPTIONS] SUBCOMMAND")
  footer("Run 'temple SUBCOMMAND --help' for more information on a command.")

  // Simplify output at the top level
  shortSubcommandsHelp(true)

  val generate = new Subcommand("generate") {
    var filename = trailArg[String]("filename", "Templefile to generate from")
  }
  addSubcommand(generate)
  verify()
}

object TempleConfig {
  class UnhandledArgumentException extends RuntimeException
}
