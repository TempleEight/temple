import generate.database.PostgresGenerator
import generate.database.ast._
import utils.FileUtils

object Main extends App {

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

  FileUtils.writeToFile("test.sql", PostgresGenerator.generate(statement))
}
