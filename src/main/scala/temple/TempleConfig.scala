package temple

import org.rogach.scallop.{ScallopConf, ScallopOption, Subcommand}

class TempleConfig(arguments: Seq[String]) extends ScallopConf(arguments) {
  version("temple 0.1 (c) 2020 TempleEight")
  banner("Usage:\ttemple [OPTIONS] SUBCOMMAND")
  footer("Run 'temple SUBCOMMAND --help' for more information on a command.")

  // Simplify output at the top level
  shortSubcommandsHelp(true)

  val generate = new Subcommand("generate") {
    var filename = trailArg[String]("filenames", "Templefiles to generate from")
  }
  addSubcommand(generate)
  verify()
}
