package generate

import generate.database.ast._

package object database {

  /** Static testing assets for DB generation */
  object TestData {

    val createStatement = Create(
      "Users",
      List(
        Column("id", Some(ColType.IntCol)),
        Column("bankBalance", Some(ColType.FloatCol)),
        Column("name", Some(ColType.StringCol)),
        Column("isStudent", Some(ColType.BoolCol)),
        Column("dateOfBirth", Some(ColType.DateCol)),
        Column("timeOfDay", Some(ColType.TimeCol)),
        Column("expiry", Some(ColType.DateTimeTzCol))
      )
    )

    val createString: String =
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
      List(
        Column("id"),
        Column("bankBalance"),
        Column("name"),
        Column("isStudent"),
        Column("dateOfBirth"),
        Column("timeOfDay"),
        Column("expiry")
      ),
      "Users"
    )

    val readString: String =
      """SELECT
      |  id,
      |  bankBalance,
      |  name,
      |  isStudent,
      |  dateOfBirth,
      |  timeOfDay,
      |  expiry
      |FROM
      |  Users;
      |""".stripMargin
  }
}
