import generate.database.PostgresGenerator
import generate.database.ast._
import utils.utils // TODO: Rename

object Main extends App {

  def square(x: Int): Int =
    x * x

  println("Hello, Temple!")

  val statement = Create(
    "Users",
    List(
      StringColumn("username"),
      StringColumn("email"),
      StringColumn("firstName"),
      StringColumn("lastName"),
      DateTimeTzColumn("createdAt"),
      IntColumn("numberOfDogs"),
      BoolColumn("yeets"),
      FloatColumn("currentBankBalance"),
      DateColumn("birthDate"),
      TimeColumn("breakfastTime")
    )
  )

  utils.writeToFile("test.sql", PostgresGenerator.generate(statement))
}
