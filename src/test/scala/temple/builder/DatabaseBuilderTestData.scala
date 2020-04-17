package temple.builder

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint.{Check, NonNull, Unique}
import temple.generate.database.ast.ComparisonOperator.{GreaterEqual, LessEqual}
import temple.generate.database.ast.Condition.PreparedComparison
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._

object DatabaseBuilderTestData {

  val sampleServiceCreate: Seq[Statement.Create] =
    Seq(
      Create(
        "temple_user",
        Seq(
          ColumnDef("id", IntCol(4), Seq(NonNull)),
          ColumnDef("bank_balance", FloatCol(8), Seq(NonNull)),
          ColumnDef("name", StringCol, Seq(NonNull)),
          ColumnDef("is_student", BoolCol, Seq(NonNull)),
          ColumnDef("date_of_birth", DateCol, Seq(NonNull)),
          ColumnDef("time_of_day", TimeCol, Seq(NonNull)),
          ColumnDef("expiry", DateTimeTzCol, Seq(NonNull)),
          ColumnDef("image", BlobCol, Seq(NonNull)),
          ColumnDef("fk", UUIDCol, Seq(NonNull)),
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
            "another_id",
            IntCol(4),
            Seq(Check("another_id", LessEqual, "100"), Check("another_id", GreaterEqual, "10"), NonNull),
          ),
          ColumnDef(
            "yet_another_id",
            IntCol(8),
            Seq(Check("yet_another_id", LessEqual, "100"), Check("yet_another_id", GreaterEqual, "10"), NonNull),
          ),
          ColumnDef(
            "bank_balance",
            FloatCol(4),
            Seq(Check("bank_balance", LessEqual, "300.0"), Check("bank_balance", GreaterEqual, "0.0"), NonNull),
          ),
          ColumnDef(
            "big_bank_balance",
            FloatCol(8),
            Seq(Check("big_bank_balance", LessEqual, "123.0"), Check("big_bank_balance", GreaterEqual, "0.0"), NonNull),
          ),
          ColumnDef("name", StringCol, Seq(Check("length(name)", GreaterEqual, "1"), NonNull)),
          ColumnDef("initials", BoundedStringCol(5), Seq(Check("length(initials)", GreaterEqual, "0"), NonNull)),
          ColumnDef("is_student", BoolCol, Seq(NonNull)),
          ColumnDef("date_of_birth", DateCol, Seq(NonNull)),
          ColumnDef("time_of_day", TimeCol, Seq(NonNull)),
          ColumnDef("expiry", DateTimeTzCol, Seq(NonNull)),
          ColumnDef("image", BlobCol, Seq(NonNull)),
          ColumnDef("sample_fk1", UUIDCol, Seq(NonNull)),
          ColumnDef("sample_fk2", UUIDCol, Seq(NonNull)),
        ),
      ),
      Create(
        "test",
        Seq(
          ColumnDef("favourite_colour", StringCol, Seq(Unique, NonNull)),
          ColumnDef("bed_time", TimeCol),
          ColumnDef(
            "favourite_number",
            IntCol(4),
            Seq(Check("favourite_number", LessEqual, "10"), Check("favourite_number", GreaterEqual, "0"), NonNull),
          ),
        ),
      ),
    )

  val sampleInsertStatement: Statement = Insert(
    "test_service",
    Seq(
      Assignment(Column("id")),
      Assignment(Column("bank_balance")),
      Assignment(Column("name")),
      Assignment(Column("is_student")),
      Assignment(Column("date_of_birth")),
      Assignment(Column("time_of_day")),
      Assignment(Column("expiry")),
      Assignment(Column("image")),
      Assignment(Column("fk")),
    ),
    returnColumns = Seq(
      Column("id"),
      Column("bank_balance"),
      Column("name"),
      Column("is_student"),
      Column("date_of_birth"),
      Column("time_of_day"),
      Column("expiry"),
      Column("image"),
      Column("fk"),
    ),
  )

  val sampleReadStatement: Statement = Read(
    "test_service",
    Seq(
      Column("id"),
      Column("bank_balance"),
      Column("name"),
      Column("is_student"),
      Column("date_of_birth"),
      Column("time_of_day"),
      Column("expiry"),
      Column("image"),
      Column("fk"),
    ),
    Some(PreparedComparison("id", ComparisonOperator.Equal)),
  )

  val sampleUpdateStatement: Statement = Update(
    "test_service",
    Seq(
      Assignment(Column("id")),
      Assignment(Column("bank_balance")),
      Assignment(Column("name")),
      Assignment(Column("is_student")),
      Assignment(Column("date_of_birth")),
      Assignment(Column("time_of_day")),
      Assignment(Column("expiry")),
      Assignment(Column("image")),
      Assignment(Column("fk")),
    ),
    Some(PreparedComparison("id", ComparisonOperator.Equal)),
    returnColumns = Seq(
      Column("id"),
      Column("bank_balance"),
      Column("name"),
      Column("is_student"),
      Column("date_of_birth"),
      Column("time_of_day"),
      Column("expiry"),
      Column("image"),
      Column("fk"),
    ),
  )

  val sampleDeleteStatement: Statement = Delete(
    "test_service",
    Some(PreparedComparison("id", ComparisonOperator.Equal)),
  )

  val sampleListStatementEnumerateByCreator: Statement = Read(
    "test_service",
    Seq(
      Column("id"),
      Column("bank_balance"),
      Column("name"),
      Column("is_student"),
      Column("date_of_birth"),
      Column("time_of_day"),
      Column("expiry"),
      Column("image"),
      Column("fk"),
    ),
    Some(PreparedComparison("created_by", ComparisonOperator.Equal)),
  )

  val sampleListStatementEnumerateByAll: Statement = Read(
    "test_service",
    Seq(
      Column("id"),
      Column("bank_balance"),
      Column("name"),
      Column("is_student"),
      Column("date_of_birth"),
      Column("time_of_day"),
      Column("expiry"),
      Column("image"),
      Column("fk"),
    ),
  )
}
