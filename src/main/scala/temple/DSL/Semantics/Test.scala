package temple.DSL.Semantics

import temple.DSL.DSLProcessor
import temple.utils.FileUtils.readFile
import temple.utils.MonadUtils.FromEither

object Test {

  //TODO: don’t let this be PR’ed
  def main(args: Array[String]): Unit = {
    val source        = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult   = DSLProcessor.parse(source).fromEither(msg => throw new Exception(s"first parse failed, $msg"))
    val semanticParse = parseSemantics(parseResult)

    println(semanticParse)
  }
}
