package generate

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint._
import temple.generate.database.ast.Comparison._
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._

package object database {

  /** Static testing assets for DB generation */
  object TestData {

    val createStatement = Create(
      "Users",
      List(
        ColumnDef("id", IntCol),
        ColumnDef("bankBalance", FloatCol),
        ColumnDef("name", StringCol),
        ColumnDef("isStudent", BoolCol),
        ColumnDef("dateOfBirth", DateCol),
        ColumnDef("timeOfDay", TimeCol),
        ColumnDef("expiry", DateTimeTzCol)
      )
    )

    val postgresCreateString: String =
      """CREATE TABLE Users (
        |  id INT,
        |  bankBalance REAL,
        |  name TEXT,
        |  isStudent BOOLEAN,
        |  dateOfBirth DATE,
        |  timeOfDay TIME,
        |  expiry TIMESTAMPTZ
        |);
        |""".stripMargin

    val createStatementWithConstraints = Create(
      "Test",
      List(
        ColumnDef("item_id", IntCol, List(NonNull, PrimaryKey)),
        ColumnDef("createdAt", DateTimeTzCol, List(Unique)),
        ColumnDef("bookingTime", TimeCol, List(References("Bookings", "bookingTime"))),
        ColumnDef("value", IntCol, List(Check("value", GreaterEqual, "1"), Null))
      )
    )

    val postgresCreateStringWithConstraints: String =
      """CREATE TABLE Test (
        |  item_id INT NOT NULL PRIMARY KEY,
        |  createdAt TIMESTAMPTZ UNIQUE,
        |  bookingTime TIME REFERENCES Bookings(bookingTime),
        |  value INT CHECK (value >= 1) NULL
        |);
        |""".stripMargin

    val readStatement = Read(
      "Users",
      List(
        Column("id"),
        Column("bankBalance"),
        Column("name"),
        Column("isStudent"),
        Column("dateOfBirth"),
        Column("timeOfDay"),
        Column("expiry")
      )
    )

    val postgresSelectString: String =
      """SELECT id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry FROM Users;
        |""".stripMargin
  }
}
