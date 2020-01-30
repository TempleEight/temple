package generate

import generate.database.ast._

package object database {

  /** Static testing assets for DB generation */
  object TestData {

    val createStatement = Create(
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

    val createString: String =
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

    val createStatementWithConstraints = Create(
      "Test",
      List(
        IntColumn("item_id", List(NonNull, PrimaryKey)),
        DateTimeTzColumn("createdAt", List(Unique)),
        TimeColumn("bookingTime", List(References("Bookings", "bookingTime"))),
        IntColumn("value", List(Check("value", GreaterEqual, "1"), Null))
      )
    )

    val createStringWithConstraints: String =
      """CREATE TABLE Test (
        |    item_id INT NOT NULL PRIMARY KEY,
        |    createdAt TIMESTAMPTZ UNIQUE,
        |    bookingTime TIME REFERENCES Bookings(bookingTime),
        |    value INT CHECK (value >= 1) NULL
        |);
        |""".stripMargin
  }
}
