import generate.database.PostgresGenerator
import generate.GenerationOutputter
import generate.database.ast._

object Main extends App {
  def square(x: Int): Int =
    x * x

  println("Hello, Temple!")

  val statement = Create("Users", List(IntColumn("yeets"), StringColumn("username"), StringColumn("email")))
  val postgresGen = new PostgresGenerator()
  postgresGen.generate(statement)

  val outputter = new GenerationOutputter("Test.SQL")
  outputter.output(postgresGen)
  outputter.close()
}
