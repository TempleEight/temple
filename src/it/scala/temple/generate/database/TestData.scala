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
    "temple_user",
    Seq(
      ColumnDef("id", IntCol(2), Seq(Unique)),
      ColumnDef("anotherId", IntCol(4), Seq(Unique)),
      ColumnDef("yetAnotherId", IntCol(6), Seq(Unique)),
      ColumnDef("bankBalance", FloatCol(4)),
      ColumnDef("bigBankBalance", FloatCol(8)),
      ColumnDef("name", StringCol),
      ColumnDef("initials", BoundedStringCol(5)),
      ColumnDef("isStudent", BoolCol),
      ColumnDef("dateOfBirth", DateCol),
      ColumnDef("timeOfDay", TimeCol),
      ColumnDef("expiry", DateTimeTzCol),
    ),
  )

  val createStatementWithUniqueConstraint: Create = Create(
    "unique_test",
    Seq(
      ColumnDef("itemID", IntCol(4), Seq(NonNull, PrimaryKey)),
      ColumnDef("createdAt", DateTimeTzCol, Seq(Unique)),
    ),
  )

  val createStatementWithReferenceConstraint: Create = Create(
    "reference_test",
    Seq(
      ColumnDef("itemID", IntCol(4), Seq(NonNull, PrimaryKey)),
      ColumnDef("userID", IntCol(4), Seq(References("temple_user", "id"))),
    ),
  )

  val createStatementWithCheckConstraint: Create = Create(
    "check_test",
    Seq(
      ColumnDef("itemID", IntCol(4), Seq(NonNull, PrimaryKey)),
      ColumnDef("value", IntCol(4), Seq(Check("value", GreaterEqual, "1"), Check("value", LessEqual, "10"))),
    ),
  )

  val readStatement: Read = Read(
    "temple_user",
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

  val readStatementWithWhere: Read = Read(
    "temple_user",
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
    Some(
      Comparison("user.id", Equal, "123456"),
    ),
  )

  val readStatementWithWhereConjunction: Read = Read(
    "temple_user",
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
    Some(
      Conjunction(
        Comparison("temple_user.id", Equal, "123456"),
        Comparison("temple_user.expiry", LessEqual, "2"),
      ),
    ),
  )

  val readStatementWithWhereDisjunction: Read = Read(
    "temple_user",
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
    Some(
      Disjunction(
        Comparison("temple_user.id", NotEqual, "123456"),
        Comparison("temple_user.expiry", Greater, "2"),
      ),
    ),
  )

  val readStatementWithWhereInverse: Read = Read(
    "temple_user",
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
    Some(
      Inverse(
        Comparison("temple_user.id", Less, "123456"),
      ),
    ),
  )

  val readStatementComplex: Read = Read(
    "temple_user",
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
    Some(
      Conjunction(
        Disjunction(
          IsNull(Column("isStudent")),
          Comparison("temple_user.id", GreaterEqual, "1"),
        ),
        Disjunction(
          Inverse(IsNull(Column("isStudent"))),
          Inverse(Comparison("temple_user.expiry", Less, "TIMESTAMP '2020-02-03 00:00:00+00'")),
        ),
      ),
    ),
  )

  val insertStatement: Insert = Insert(
    "temple_user",
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

  val insertStatementForUniqueConstraint: Insert = Insert(
    "unique_test",
    Seq(
      Column("itemID"),
      Column("createdAt"),
    ),
  )

  val insertStatementForReferenceConstraint: Insert = Insert(
    "reference_test",
    Seq(
      Column("itemID"),
      Column("userID"),
    ),
  )

  val insertStatementForCheckConstraint: Insert = Insert(
    "check_test",
    Seq(
      Column("itemID"),
      Column("value"),
    ),
  )

  val deleteStatement: Delete = Delete(
    "temple_user",
  )

  val deleteStatementWithWhere: Delete = Delete(
    "temple_user",
    Some(
      Comparison("temple_user.id", Equal, "123456"),
    ),
  )

  val dropStatement: Drop = Drop(
    "temple_user",
    ifExists = false,
  )

  val dropStatementIfExists: Drop = Drop(
    "temple_user",
  )

  val updateStatement: Update = Update(
    "temple_user",
    Seq(
      Assignment(Column("bankBalance"), Value("123.456")),
      Assignment(Column("name"), Value("'Will'")),
    ),
  )

  val updateStatementWithWhere: Update = Update(
    "temple_user",
    Seq(
      Assignment(Column("bankBalance"), Value("123.456")),
      Assignment(Column("name"), Value("'Will'")),
    ),
    Some(
      Comparison("temple_user.id", Equal, "12345"),
    ),
  )

  val insertDataA: Seq[PreparedVariable] = Seq(
    PreparedVariable.ShortVariable(3),
    PreparedVariable.IntVariable(4),
    PreparedVariable.LongVariable(5),
    PreparedVariable.FloatVariable(100.1f),
    PreparedVariable.DoubleVariable(1000.2f),
    PreparedVariable.StringVariable("John Smith"),
    PreparedVariable.StringVariable("ABC"),
    PreparedVariable.BoolVariable(true),
    PreparedVariable.DateVariable(Date.valueOf("1998-03-05")),
    PreparedVariable.TimeVariable(Time.valueOf("12:00:00")),
    PreparedVariable.DateTimeTzVariable(Timestamp.valueOf("2020-01-01 00:00:00.0")),
  )

  val insertDataB: Seq[PreparedVariable] = Seq(
    PreparedVariable.ShortVariable(12345),
    PreparedVariable.IntVariable(123456),
    PreparedVariable.LongVariable(1234567),
    PreparedVariable.FloatVariable(23.42f),
    PreparedVariable.DoubleVariable(3.141592f),
    PreparedVariable.StringVariable("Jane Doe"),
    PreparedVariable.StringVariable("WJVS"),
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
