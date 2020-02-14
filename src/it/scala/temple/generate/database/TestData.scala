package temple.generate.database

import java.sql.{Date, Time, Timestamp}

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint._
import temple.generate.database.ast.ComparisonOperator._
import temple.generate.database.ast.Condition._
import temple.generate.database.ast.Expression.Value
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._

/** Static testing assets for DB generation */
object TestData {

  val createStatement: Create = Create(
    "Users",
    Seq(
      ColumnDef("id", IntCol, Seq(Unique)),
      ColumnDef("bankBalance", FloatCol),
      ColumnDef("name", StringCol),
      ColumnDef("isStudent", BoolCol),
      ColumnDef("dateOfBirth", DateCol),
      ColumnDef("timeOfDay", TimeCol),
      ColumnDef("expiry", DateTimeTzCol),
    ),
  )

  val createStatementWithUniqueConstraint: Create = Create(
    "TestUnique",
    Seq(
      ColumnDef("itemID", IntCol, Seq(NonNull, PrimaryKey)),
      ColumnDef("createdAt", DateTimeTzCol, Seq(Unique)),
    ),
  )

  val createStatementWithReferenceConstraint: Create = Create(
    "TestReference",
    Seq(
      ColumnDef("itemID", IntCol, Seq(NonNull, PrimaryKey)),
      ColumnDef("userID", IntCol, Seq(References("Users", "id"))),
    ),
  )

  val createStatementWithCheckConstraint: Create = Create(
    "TestCheck",
    Seq(
      ColumnDef("itemID", IntCol, Seq(NonNull, PrimaryKey)),
      ColumnDef("value", IntCol, Seq(Check("value", GreaterEqual, "1"), Check("value", LessEqual, "10"))),
    ),
  )

  val readStatement: Read = Read(
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

  val insertStatementForUniqueConstraint: Insert = Insert(
    "TestUnique",
    Seq(
      Column("itemID"),
      Column("createdAt"),
    ),
  )

  val insertStatementForReferenceConstraint: Insert = Insert(
    "TestReference",
    Seq(
      Column("itemID"),
      Column("userID"),
    ),
  )

  val insertStatementForCheckConstraint: Insert = Insert(
    "TestCheck",
    Seq(
      Column("itemID"),
      Column("value"),
    ),
  )

  val deleteStatement: Delete = Delete(
    "Users",
  )

  val deleteStatementWithWhere: Delete = Delete(
    "Users",
    Some(
      Comparison("Users.id", Equal, "123456"),
    ),
  )

  val dropStatement: Drop = Drop(
    "Users",
    ifExists = false,
  )

  val dropStatementIfExists: Drop = Drop(
    "Users",
  )

  val updateStatement: Update = Update(
    "Users",
    Seq(
      Assignment(Column("bankBalance"), Value("123.456")),
      Assignment(Column("name"), Value("'Will'")),
    ),
  )

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

  val insertDataA: Seq[PreparedVariable] = Seq(
    PreparedVariable.IntVariable(3),
    PreparedVariable.FloatVariable(100.1f),
    PreparedVariable.StringVariable("John Smith"),
    PreparedVariable.BoolVariable(true),
    PreparedVariable.DateVariable(Date.valueOf("1998-03-05")),
    PreparedVariable.TimeVariable(Time.valueOf("12:00:00")),
    PreparedVariable.DateTimeTzVariable(Timestamp.valueOf("2020-01-01 00:00:00.0")),
  )

  val insertDataB: Seq[PreparedVariable] = Seq(
    PreparedVariable.IntVariable(123456),
    PreparedVariable.FloatVariable(23.42f),
    PreparedVariable.StringVariable("Jane Doe"),
    PreparedVariable.BoolVariable(false),
    PreparedVariable.DateVariable(Date.valueOf("1998-03-05")),
    PreparedVariable.TimeVariable(Time.valueOf("12:00:00")),
    PreparedVariable.DateTimeTzVariable(Timestamp.valueOf("2019-02-03 02:23:50.0")),
  )

  val insertDataUniqueConstraintA: Seq[PreparedVariable] = Seq(
    PreparedVariable.IntVariable(0),
    PreparedVariable.DateTimeTzVariable(Timestamp.valueOf("2019-11-14 01:02:03.0")),
  )

  val insertDataUniqueConstraintB: Seq[PreparedVariable] = Seq(
    PreparedVariable.IntVariable(1),
    PreparedVariable.DateTimeTzVariable(Timestamp.valueOf("2019-11-14 01:02:03.0")),
  )

  val insertDataReferenceConstraintA: Seq[PreparedVariable] = Seq(
    PreparedVariable.IntVariable(1),
    PreparedVariable.IntVariable(3),
  )

  val insertDataReferenceConstraintB: Seq[PreparedVariable] = Seq(
    PreparedVariable.IntVariable(1),
    PreparedVariable.IntVariable(123456789),
  )

  val insertDataCheckConstraintLowerFails: Seq[PreparedVariable] = Seq(
    PreparedVariable.IntVariable(1),
    PreparedVariable.IntVariable(0),
  )

  val insertDataCheckConstraintUpperFails: Seq[PreparedVariable] = Seq(
    PreparedVariable.IntVariable(1),
    PreparedVariable.IntVariable(11),
  )

  val insertDataCheckConstraintPasses: Seq[PreparedVariable] = Seq(
    PreparedVariable.IntVariable(1),
    PreparedVariable.IntVariable(5),
  )

}
