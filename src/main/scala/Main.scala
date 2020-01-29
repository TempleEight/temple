import scala.io.Source
import temple.DSL.DSLParser

object Main extends App {
  val fSource = Source.fromFile(args(0))
  val result  = DSLParser.parse(fSource.reader())
  println(result)

  fSource.close()
}
