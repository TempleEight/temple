package temple.generate.database

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint._
import temple.generate.database.ast.ComparisonOperator._
import temple.generate.database.ast.Condition._
import temple.generate.database.ast.Expression._
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._

/** Static testing assets for DB generation */
object PostgresGeneratorTestData {

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
      ColumnDef("veryUnique", UUIDCol),
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
        |  picture BYTEA,
        |  veryUnique UUID
        |);""".stripMargin

  val createStatementWithConstraints: Create = Create(
    "Test",
    Seq(
      ColumnDef("item_id", IntCol(4), Seq(NonNull, PrimaryKey)),
      ColumnDef("createdAt", DateTimeTzCol, Seq(Unique)),
      ColumnDef("bookingTime", TimeCol, Seq(References("Bookings", "bookingTime"))),
      ColumnDef("value", IntCol(4), Seq(Check("value", GreaterEqual, "1"), NonNull)),
    ),
  )

  val postgresCreateStringWithConstraints: String =
    """CREATE TABLE Test (
        |  item_id INT NOT NULL PRIMARY KEY,
        |  createdAt TIMESTAMPTZ UNIQUE,
        |  bookingTime TIME REFERENCES Bookings (bookingTime),
        |  value INT CHECK (value >= 1) NOT NULL
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
      Column("veryUnique"),
    ),
  )

  val postgresSelectString: String =
    """SELECT id, anotherId, yetAnotherId, bankBalance, bigBankBalance, name, initials, isStudent, dateOfBirth, timeOfDay, expiry, veryUnique FROM Users;"""

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

  val readStatementWithWherePrepared: Read = Read(
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
      PreparedComparison("Users.id", Equal),
    ),
  )

  val postgresSelectStringWithWherePrepared: String =
    """SELECT id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry FROM Users WHERE Users.id = $1;"""

  val readStatementWithNestedWherePrepared: Read = Read(
    "Users",
    Seq(Column("bankBalance")),
    Some(
      Conjunction(
        Disjunction(
          Inverse(PreparedComparison("Users.id", LessEqual)),
          PreparedComparison("Users.isStudent", Equal),
        ),
        PreparedComparison("Users.timeOfDay", Equal),
      ),
    ),
  )

  val postgresSelectStringWithNestedWherePrepared: String =
    """SELECT bankBalance FROM Users WHERE ((NOT (Users.id <= $1)) OR (Users.isStudent = $2)) AND (Users.timeOfDay = $3);"""

  val postgresSelectStringWithNestedWherePreparedUsingQuestionMarks: String =
    """SELECT bankBalance FROM Users WHERE ((NOT (Users.id <= ?)) OR (Users.isStudent = ?)) AND (Users.timeOfDay = ?);"""

  val insertStatement: Insert = Insert(
    "Users",
    Seq(
      Assignment(Column("id"), PreparedValue),
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), PreparedValue),
      Assignment(Column("isStudent"), PreparedValue),
      Assignment(Column("dateOfBirth"), PreparedValue),
      Assignment(Column("timeOfDay"), PreparedValue),
      Assignment(Column("expiry"), PreparedValue),
    ),
  )

  val postgresInsertString: String =
    "INSERT INTO Users (id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry) VALUES ($1, $2, $3, $4, $5, $6, $7);"

  val postgresInsertStringWithQuestionMarks: String =
    "INSERT INTO Users (id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry) VALUES (?, ?, ?, ?, ?, ?, ?);"

  val insertStatementWithReturn: Insert = Insert(
    "Users",
    Seq(
      Assignment(Column("id"), PreparedValue),
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), PreparedValue),
      Assignment(Column("isStudent"), PreparedValue),
      Assignment(Column("dateOfBirth"), PreparedValue),
      Assignment(Column("timeOfDay"), PreparedValue),
      Assignment(Column("expiry"), PreparedValue),
    ),
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

  val postgresInsertStringWithReturn: String =
    "INSERT INTO Users (id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry;"

  val insertStatementWithProvidedValue: Insert = Insert(
    "Users",
    Seq(
      Assignment(Column("id"), Value("180")),
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), PreparedValue),
      Assignment(Column("isStudent"), PreparedValue),
      Assignment(Column("dateOfBirth"), PreparedValue),
      Assignment(Column("timeOfDay"), PreparedValue),
      Assignment(Column("expiry"), PreparedValue),
    ),
  )

  val postgresInsertStringWithProvidedValue: String =
    "INSERT INTO Users (id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry) VALUES (180, $1, $2, $3, $4, $5, $6);"

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

  val updateStatementWithReturn: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), Value("123.456")),
      Assignment(Column("name"), Value("'Will'")),
    ),
    None,
    Seq(
      Column("bankBalance"),
    ),
  )

  val postgresUpdateStringWithReturn: String =
    """UPDATE Users SET bankBalance = 123.456, name = 'Will' RETURNING bankBalance;"""

  val updateStatementWithWhereAndReturn: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), Value("123.456")),
      Assignment(Column("name"), Value("'Will'")),
    ),
    Some(
      Comparison("Users.id", Equal, "123456"),
    ),
    Seq(
      Column("bankBalance"),
    ),
  )

  val postgresUpdateStringWithWhereAndReturn: String =
    """UPDATE Users SET bankBalance = 123.456, name = 'Will' WHERE Users.id = 123456 RETURNING bankBalance;"""

  val updateStatementPrepared: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), PreparedValue),
    ),
    None,
  )

  val postgresUpdateStringPrepared: String =
    """UPDATE Users SET bankBalance = $1, name = $2;"""

  val postgresUpdateStringPreparedWithQuestionMarks: String =
    """UPDATE Users SET bankBalance = ?, name = ?;"""

  val updateStatementPreparedWithWhere: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), PreparedValue),
    ),
    Some(
      Comparison("Users.id", Equal, "123456"),
    ),
  )

  val postgresUpdateStringPreparedWithWhere: String =
    """UPDATE Users SET bankBalance = $1, name = $2 WHERE Users.id = 123456;"""

  val updateStatementPreparedWithWherePrepared: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), PreparedValue),
    ),
    Some(
      PreparedComparison("Users.id", Equal),
    ),
  )

  val postgresUpdateStringPreparedWithWherePrepared: String =
    """UPDATE Users SET bankBalance = $1, name = $2 WHERE Users.id = $3;"""

  val updateStatementPreparedWithNestedWherePrepared: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), PreparedValue),
    ),
    Some(
      Conjunction(
        Disjunction(
          Inverse(PreparedComparison("Users.id", LessEqual)),
          PreparedComparison("Users.isStudent", Equal),
        ),
        PreparedComparison("Users.timeOfDay", Equal),
      ),
    ),
  )

  val postgresUpdateStringPreparedWithNestedWherePrepared: String =
    """UPDATE Users SET bankBalance = $1, name = $2 WHERE ((NOT (Users.id <= $3)) OR (Users.isStudent = $4)) AND (Users.timeOfDay = $5);"""

  val updateStatementPreparedAndValuesWithNestedWherePrepared: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), Value("'Hello'")),
    ),
    Some(
      Conjunction(
        Disjunction(
          Inverse(PreparedComparison("Users.id", LessEqual)),
          PreparedComparison("Users.isStudent", Equal),
        ),
        PreparedComparison("Users.timeOfDay", Equal),
      ),
    ),
  )

  val postgresUpdateStringPreparedAndValuesWithNestedWherePrepared: String =
    """UPDATE Users SET bankBalance = $1, name = 'Hello' WHERE ((NOT (Users.id <= $2)) OR (Users.isStudent = $3)) AND (Users.timeOfDay = $4);"""

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

  val deleteStatementWithWherePrepared: Delete = Delete(
    "Users",
    Some(
      PreparedComparison("Users.id", Equal),
    ),
  )

  val postgresDeleteStringWithWherePrepared: String =
    """DELETE FROM Users WHERE Users.id = $1;"""

  val deleteStatementWithNestedWherePrepared: Delete = Delete(
    "Users",
    Some(
      Conjunction(
        Disjunction(
          Inverse(PreparedComparison("Users.id", LessEqual)),
          PreparedComparison("Users.isStudent", Equal),
        ),
        PreparedComparison("Users.timeOfDay", Equal),
      ),
    ),
  )

  val postgresDeleteStringWithNestedWherePrepared: String =
    """DELETE FROM Users WHERE ((NOT (Users.id <= $1)) OR (Users.isStudent = $2)) AND (Users.timeOfDay = $3);"""

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

  // Examples from spec-golang
  val specGolangList: Read = Read(
    "match",
    Seq(
      Column("id"),
      Column("created_by"),
      Column("userOne"),
      Column("userTwo"),
      Column("matchedOn"),
    ),
    Some(
      PreparedComparison("created_by", Equal),
    ),
  )

  val specGolangListPostgresString: String =
    """SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE created_by = $1;"""

  val specGolangCreate: Insert = Insert(
    "match",
    Seq(
      Assignment(Column("id"), PreparedValue),
      Assignment(Column("created_by"), PreparedValue),
      Assignment(Column("userOne"), PreparedValue),
      Assignment(Column("userTwo"), PreparedValue),
      Assignment(Column("matchedOn"), Value("NOW()")),
    ),
    Seq(
      Column("id"),
      Column("created_by"),
      Column("userOne"),
      Column("userTwo"),
      Column("matchedOn"),
    ),
  )

  val specGolangCreatePostgresString: String =
    """INSERT INTO match (id, created_by, userOne, userTwo, matchedOn) VALUES ($1, $2, $3, $4, NOW()) RETURNING id, created_by, userOne, userTwo, matchedOn;"""

  val specGolangRead: Read = Read(
    "match",
    Seq(
      Column("id"),
      Column("created_by"),
      Column("userOne"),
      Column("userTwo"),
      Column("matchedOn"),
    ),
    Some(
      PreparedComparison("id", Equal),
    ),
  )

  val specGolangReadPostgresString: String =
    """SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE id = $1;"""

  val specGolangUpdate: Update = Update(
    "match",
    Seq(
      Assignment(Column("userOne"), PreparedValue),
      Assignment(Column("userTwo"), PreparedValue),
      Assignment(Column("matchedOn"), Value("NOW()")),
    ),
    Some(
      PreparedComparison("id", Equal),
    ),
    Seq(
      Column("id"),
      Column("created_by"),
      Column("userOne"),
      Column("userTwo"),
      Column("matchedOn"),
    ),
  )

  val specGolangUpdatePostgresString: String =
    """UPDATE match SET userOne = $1, userTwo = $2, matchedOn = NOW() WHERE id = $3 RETURNING id, created_by, userOne, userTwo, matchedOn;"""

  val specGolangDelete: Delete = Delete(
    "match",
    Some(
      PreparedComparison(
        "id",
        Equal,
      ),
    ),
  )

  val specGolangDeletePostgresString: String =
    """DELETE FROM match WHERE id = $1;"""
}
