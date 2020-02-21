package temple.builder

import temple.DSL.semantics.AttributeType._
import temple.DSL.semantics.{Annotation, Attribute, ServiceBlock, StructBlock}
import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint.{Check, NonNull, Unique}
import temple.generate.database.ast.ComparisonOperator.{GreaterEqual, LessEqual}
import temple.generate.database.ast.Statement.Create
import temple.generate.database.ast.{ColumnDef, Statement}

import scala.collection.immutable.ListMap

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
}
