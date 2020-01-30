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
  }
}
