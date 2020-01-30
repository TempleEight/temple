package generate

import generate.database.ast._

package object database {

  /** Static testing assets for DB generation */
  object TestData {

    val createStatement = Create(
      "Users",
      List(
        ColumnDef("id", ColType.IntCol),
        ColumnDef("bankBalance", ColType.FloatCol),
        ColumnDef("name", ColType.StringCol),
        ColumnDef("isStudent", ColType.BoolCol),
        ColumnDef("dateOfBirth", ColType.DateCol),
        ColumnDef("timeOfDay", ColType.TimeCol),
        ColumnDef("expiry", ColType.DateTimeTzCol)
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
