package temple.generate.database

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint._
import temple.generate.database.ast.ComparisonOperator._
import temple.generate.database.ast.Condition._
import temple.generate.database.ast.Expression._
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._

/** Static testing assets for DB generation */
object IntegrationTestData {

  val createStatement: Create = Create(
    "Users",
    Seq(
      ColumnDef("id", IntCol(2)),
      ColumnDef("anotherId", IntCol(4)),
      ColumnDef("yetAnotherId", IntCol(6)),
      ColumnDef("bankBalance", FloatCol(4)),
      ColumnDef("bigBankBalance", FloatCol(8)),
      ColumnDef("name", StringCol),
      ColumnDef("initials", BoundedStringCol(5)),
      ColumnDef("isStudent", BoolCol),
      ColumnDef("dateOfBirth", DateCol),
      ColumnDef("timeOfDay", TimeCol),
      ColumnDef("expiry", DateTimeTzCol),
      ColumnDef("picture", BlobCol),
    ),
  )

  val postgresCreateString: String =
    """CREATE TABLE Users (
        |  id SMALLINT,
        |  anotherId INT,
        |  yetAnotherId BIGINT,
        |  bankBalance REAL,
        |  bigBankBalance DOUBLE PRECISION,
        |  name TEXT,
        |  initials VARCHAR(5),
        |  isStudent BOOLEAN,
        |  dateOfBirth DATE,
        |  timeOfDay TIME,
        |  expiry TIMESTAMPTZ,
        |  picture BYTEA
        |);""".stripMargin

  val createStatementWithConstraints: Create = Create(
    "Test",
    Seq(
      ColumnDef("item_id", IntCol(4), Seq(NonNull, PrimaryKey)),
      ColumnDef("createdAt", DateTimeTzCol, Seq(Unique)),
      ColumnDef("bookingTime", TimeCol, Seq(References("Bookings", "bookingTime"))),
      ColumnDef("value", IntCol(4), Seq(Check("value", GreaterEqual, "1"), Null)),
    ),
  )

  val postgresCreateStringWithConstraints: String =
    """CREATE TABLE Test (
        |  item_id INT NOT NULL PRIMARY KEY,
        |  createdAt TIMESTAMPTZ UNIQUE,
        |  bookingTime TIME REFERENCES Bookings (bookingTime),
        |  value INT CHECK (value >= 1) NULL
        |);""".stripMargin

  val readStatement: Read = Read(
    "Users",
    Seq(
      Column("id"),
      Column("anotherId"),
      Column("yetAnotherId"),
      Column("bankBalance"),
      Column("bigBankBalance"),
      Column("name"),
      Column("initials"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
    ),
  )

  val postgresSelectString: String =
    """SELECT id, anotherId, yetAnotherId, bankBalance, bigBankBalance, name, initials, isStudent, dateOfBirth, timeOfDay, expiry FROM Users;"""

  val readStatementWithWhere: Read = Read(
    "Users",
    Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
    ),
    Some(
      Comparison("Users.id", Equal, "123456"),
    ),
  )

  val postgresSelectStringWithWhere: String =
    """SELECT id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry FROM Users WHERE Users.id = 123456;"""

  val readStatementWithWhereConjunction: Read = Read(
    "Users",
    Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
    ),
    Some(
      Conjunction(
        Comparison("Users.id", Equal, "123456"),
        Comparison("Users.expiry", LessEqual, "2"),
      ),
    ),
  )

  val postgresSelectStringWithWhereConjunction: String =
    """SELECT id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry FROM Users WHERE (Users.id = 123456) AND (Users.expiry <= 2);"""

  val readStatementWithWhereDisjunction: Read = Read(
    "Users",
    Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
    ),
    Some(
      Disjunction(
        Comparison("Users.id", NotEqual, "123456"),
        Comparison("Users.expiry", Greater, "2"),
      ),
    ),
  )

  val postgresSelectStringWithWhereDisjunction: String =
    """SELECT id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry FROM Users WHERE (Users.id <> 123456) OR (Users.expiry > 2);"""

  val readStatementWithWhereInverse: Read = Read(
    "Users",
    Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
    ),
    Some(
      Inverse(
        Comparison("Users.id", Less, "123456"),
      ),
    ),
  )

  val postgresSelectStringWithWhereInverse: String =
    """SELECT id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry FROM Users WHERE NOT (Users.id < 123456);"""

  val readStatementComplex: Read = Read(
    "Users",
    Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
    ),
    Some(
      Conjunction(
        Disjunction(
          IsNull(Column("isStudent")),
          Comparison("Users.id", GreaterEqual, "1"),
        ),
        Disjunction(
          Inverse(IsNull(Column("isStudent"))),
          Inverse(Comparison("Users.expiry", Less, "TIMESTAMP '2020-02-03 00:00:00+00'")),
        ),
      ),
    ),
  )

  val postgresSelectStringComplex: String =
    "SELECT id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry FROM Users WHERE ((isStudent IS NULL) OR (Users.id >= 1)) AND ((isStudent IS NOT NULL) OR (NOT (Users.expiry < TIMESTAMP '2020-02-03 00:00:00+00')));"

  val insertStatement: Insert = Insert(
    "Users",
    Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
    ),
  )

  val postgresInsertString: String =
    "INSERT INTO Users (id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry) VALUES ($1, $2, $3, $4, $5, $6, $7);"

  val postgresInsertStringWithQuestionMarks: String =
    "INSERT INTO Users (id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry) VALUES (?, ?, ?, ?, ?, ?, ?);"

  val updateStatement: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), Value("123.456")),
      Assignment(Column("name"), Value("'Will'")),
    ),
  )

  val postgresUpdateString: String =
    """UPDATE Users SET bankBalance = 123.456, name = 'Will';"""

  val updateStatementWithWhere: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), Value("123.456")),
      Assignment(Column("name"), Value("'Will'")),
    ),
    Some(
      Comparison("Users.id", Equal, "123456"),
    ),
  )

  val postgresUpdateStringWithWhere: String =
    """UPDATE Users SET bankBalance = 123.456, name = 'Will' WHERE Users.id = 123456;"""

  val deleteStatement: Delete = Delete(
    "Users",
  )

  val postgresDeleteString: String =
    """DELETE FROM Users;"""

  val deleteStatementWithWhere: Delete = Delete(
    "Users",
    Some(
      Comparison("Users.id", Equal, "123456"),
    ),
  )

  val postgresDeleteStringWithWhere: String =
    """DELETE FROM Users WHERE Users.id = 123456;"""

  val dropStatement: Drop = Drop(
    "Users",
    ifExists = false,
  )

  val postgresDropString: String =
    """DROP TABLE Users;"""

  val dropStatementIfExists: Drop = Drop(
    "Users",
  )

  val postgresDropStringIfExists: String =
    """DROP TABLE Users IF EXISTS;"""
}
