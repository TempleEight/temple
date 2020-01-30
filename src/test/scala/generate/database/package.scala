package generate

import generate.database.ast._

package object database {
  object Assets {

    val create_Statement = Create(
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
    val create_String: String =
      """CREATE TABLE Users (
        |    username TEXT,
        |    email TEXT,
        |    firstName TEXT,
        |    lastName TEXT,
        |    createdAt TIMESTAMPTZ,
        |    numberOfDogs INT,
        |    yeets BOOLEAN,
        |    currentBankBalance REAL,
        |    birthDate DATE,
        |    breakfastTime TIME
        |);
        |""".stripMargin

  }
}
