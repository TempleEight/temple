import generate.database.PostgresGenerator
import generate.GenerationOutputter

object Main extends App {
  def square(x: Int): Int =
    x * x

  println("Hello, Temple!")

  val postgresGen = new PostgresGenerator()
  postgresGen.create("Users")
  postgresGen.create("Bookings")
  postgresGen.delete("Users")

  val outputter = new GenerationOutputter("Test.SQL")
  outputter.output(postgresGen)
  outputter.close()
}
