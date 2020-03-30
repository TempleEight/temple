package temple.builder

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint.{Check, NonNull, Unique}
import temple.generate.database.ast.ComparisonOperator.{GreaterEqual, LessEqual}
import temple.generate.database.ast.Condition.PreparedComparison
import temple.generate.database.ast.Expression.PreparedValue
import temple.generate.database.ast.Statement._
import temple.generate.database.ast.{Assignment, Column, ColumnDef, ComparisonOperator, Condition, Statement}

object DatabaseBuilderTestData {

  val sampleServiceCreate: Seq[Statement.Create] =
    Seq(
      Create(
        "temple_user",
        Seq(
          ColumnDef("id", IntCol(4), Seq(NonNull)),
          ColumnDef("bankBalance", FloatCol(8), Seq(NonNull)),
          ColumnDef("name", StringCol, Seq(NonNull)),
          ColumnDef("isStudent", BoolCol, Seq(NonNull)),
          ColumnDef("dateOfBirth", DateCol, Seq(NonNull)),
          ColumnDef("timeOfDay", TimeCol, Seq(NonNull)),
          ColumnDef("expiry", DateTimeTzCol, Seq(NonNull)),
          ColumnDef("image", BlobCol, Seq(NonNull)),
        ),
      ),
    )

  val sampleComplexServiceCreate: Seq[Statement.Create] =
    Seq(
      Create(
        "temple_user",
        Seq(
          ColumnDef("id", IntCol(2), Seq(Check("id", LessEqual, "100"), Check("id", GreaterEqual, "10"), NonNull)),
          ColumnDef(
            "anotherId",
            IntCol(4),
            Seq(Check("anotherId", LessEqual, "100"), Check("anotherId", GreaterEqual, "10"), NonNull),
          ),
          ColumnDef(
            "yetAnotherId",
            IntCol(8),
            Seq(Check("yetAnotherId", LessEqual, "100"), Check("yetAnotherId", GreaterEqual, "10"), NonNull),
          ),
          ColumnDef(
            "bankBalance",
            FloatCol(4),
            Seq(Check("bankBalance", LessEqual, "300.0"), Check("bankBalance", GreaterEqual, "0.0"), NonNull),
          ),
          ColumnDef(
            "bigBankBalance",
            FloatCol(8),
            Seq(Check("bigBankBalance", LessEqual, "123.0"), Check("bigBankBalance", GreaterEqual, "0.0"), NonNull),
          ),
          ColumnDef("name", StringCol, Seq(Check("length(name)", GreaterEqual, "1"), NonNull)),
          ColumnDef("initials", BoundedStringCol(5), Seq(Check("length(initials)", GreaterEqual, "0"), NonNull)),
          ColumnDef("isStudent", BoolCol, Seq(NonNull)),
          ColumnDef("dateOfBirth", DateCol, Seq(NonNull)),
          ColumnDef("timeOfDay", TimeCol, Seq(NonNull)),
          ColumnDef("expiry", DateTimeTzCol, Seq(NonNull)),
          ColumnDef("image", BlobCol, Seq(NonNull)),
        ),
      ),
      Create(
        "test",
        Seq(
          ColumnDef("favouriteColour", StringCol, Seq(Unique, NonNull)),
          ColumnDef("bedTime", TimeCol),
          ColumnDef(
            "favouriteNumber",
            IntCol(4),
            Seq(Check("favouriteNumber", LessEqual, "10"), Check("favouriteNumber", GreaterEqual, "0"), NonNull),
          ),
        ),
      ),
    )

  val sampleInsertStatement: Statement = Insert(
    "test-service",
    Seq(
      Assignment(Column("id"), PreparedValue),
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), PreparedValue),
      Assignment(Column("isStudent"), PreparedValue),
      Assignment(Column("dateOfBirth"), PreparedValue),
      Assignment(Column("timeOfDay"), PreparedValue),
      Assignment(Column("expiry"), PreparedValue),
      Assignment(Column("image"), PreparedValue),
    ),
    returnColumns = Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
      Column("image"),
    ),
  )

  val sampleReadStatement: Statement = Read(
    "test-service",
    Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
      Column("image"),
    ),
    Some(PreparedComparison("id", ComparisonOperator.Equal)),
  )

  val sampleUpdateStatement: Statement = Update(
    "test-service",
    Seq(
      Assignment(Column("id"), PreparedValue),
      Assignment(Column("bankBalance"), PreparedValue),
      Assignment(Column("name"), PreparedValue),
      Assignment(Column("isStudent"), PreparedValue),
      Assignment(Column("dateOfBirth"), PreparedValue),
      Assignment(Column("timeOfDay"), PreparedValue),
      Assignment(Column("expiry"), PreparedValue),
      Assignment(Column("image"), PreparedValue),
    ),
    Some(PreparedComparison("id", ComparisonOperator.Equal)),
    returnColumns = Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
      Column("image"),
    ),
  )

  val sampleDeleteStatement: Statement = Delete(
    "test-service",
    Some(PreparedComparison("id", ComparisonOperator.Equal)),
  )

  val sampleListStatementEnumerateByCreator: Statement = Read(
    "test-service",
    Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
      Column("image"),
    ),
    Some(PreparedComparison("created_by", ComparisonOperator.Equal)),
  )

  val sampleListStatementEnumerateByAll: Statement = Read(
    "test-service",
    Seq(
      Column("id"),
      Column("bankBalance"),
      Column("name"),
      Column("isStudent"),
      Column("dateOfBirth"),
      Column("timeOfDay"),
      Column("expiry"),
      Column("image"),
    ),
  )
}
