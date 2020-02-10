package temple.generate

import java.sql.{Date, Time, Timestamp}

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint._
import temple.generate.database.ast.ComparisonOperator._
import temple.generate.database.ast.Condition._
import temple.generate.database.ast.Expression.Value
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._

package object database {

  /** Static testing assets for DB generation */
  object TestData {

    val createStatement = Create(
      "Users",
      Seq(
        ColumnDef("id", IntCol),
        ColumnDef("bankBalance", FloatCol),
        ColumnDef("name", StringCol),
        ColumnDef("isStudent", BoolCol),
        ColumnDef("dateOfBirth", DateCol),
        ColumnDef("timeOfDay", TimeCol),
        ColumnDef("expiry", DateTimeTzCol)
      )
    )

    val createStatementWithConstraints = Create(
      "Test",
      Seq(
        ColumnDef("itemID", IntCol, Seq(NonNull, PrimaryKey)),
        ColumnDef("createdAt", DateTimeTzCol, Seq(Unique)),
        ColumnDef("bookingTime", TimeCol, Seq(References("Bookings", "bookingTime"))),
        ColumnDef("value", IntCol, Seq(Check("value", GreaterEqual, "1"), Null))
      )
    )

    val readStatement = Read(
      "Users",
      Seq(
        Column("id"),
        Column("bankBalance"),
        Column("name"),
        Column("isStudent"),
        Column("dateOfBirth"),
        Column("timeOfDay"),
        Column("expiry")
      )
    )

    val readStatementWithWhere = Read(
      "Users",
      Seq(
        Column("id"),
        Column("bankBalance"),
        Column("name"),
        Column("isStudent"),
        Column("dateOfBirth"),
        Column("timeOfDay"),
        Column("expiry")
      ),
      Some(
        Comparison("Users.id", Equal, "123456")
      )
    )

    val readStatementWithWhereConjunction = Read(
      "Users",
      Seq(
        Column("id"),
        Column("bankBalance"),
        Column("name"),
        Column("isStudent"),
        Column("dateOfBirth"),
        Column("timeOfDay"),
        Column("expiry")
      ),
      Some(
        Conjunction(
          Comparison("Users.id", Equal, "123456"),
          Comparison("Users.expiry", LessEqual, "2")
        )
      )
    )

    val readStatementWithWhereDisjunction = Read(
      "Users",
      Seq(
        Column("id"),
        Column("bankBalance"),
        Column("name"),
        Column("isStudent"),
        Column("dateOfBirth"),
        Column("timeOfDay"),
        Column("expiry")
      ),
      Some(
        Disjunction(
          Comparison("Users.id", NotEqual, "123456"),
          Comparison("Users.expiry", Greater, "2")
        )
      )
    )

    val readStatementWithWhereInverse = Read(
      "Users",
      Seq(
        Column("id"),
        Column("bankBalance"),
        Column("name"),
        Column("isStudent"),
        Column("dateOfBirth"),
        Column("timeOfDay"),
        Column("expiry")
      ),
      Some(
        Inverse(
          Comparison("Users.id", Less, "123456")
        )
      )
    )

    val readStatementComplex = Read(
      "Users",
      Seq(
        Column("id"),
        Column("bankBalance"),
        Column("name"),
        Column("isStudent"),
        Column("dateOfBirth"),
        Column("timeOfDay"),
        Column("expiry")
      ),
      Some(
        Conjunction(
          Disjunction(
            IsNull(Column("isStudent")),
            Comparison("Users.id", GreaterEqual, "1")
          ),
          Disjunction(
            Inverse(IsNull(Column("isStudent"))),
            Inverse(Comparison("Users.expiry", Less, "TIMESTAMP '2020-02-03 00:00:00+00'"))
          )
        )
      )
    )

    val insertStatement = Insert(
      "Users",
      Seq(
        Column("id"),
        Column("bankBalance"),
        Column("name"),
        Column("isStudent"),
        Column("dateOfBirth"),
        Column("timeOfDay"),
        Column("expiry")
      )
    )

    val deleteStatement = Delete(
      "Users"
    )

    val deleteStatementWithWhere = Delete(
      "Users",
      Some(
        Comparison("Users.id", Equal, "123456")
      )
    )

    val dropStatement = Drop(
      "Users",
      ifExists = false
    )

    val dropStatementIfExists = Drop(
      "Users"
    )

    val updateStatement = Update(
      "Users",
      Seq(
        Assignment(Column("bankBalance"), Value("123.456")),
        Assignment(Column("name"), Value("'Will'"))
      )
    )

    val updateStatementWithWhere = Update(
      "Users",
      Seq(
        Assignment(Column("bankBalance"), Value("123.456")),
        Assignment(Column("name"), Value("'Will'"))
      ),
      Some(
        Comparison("Users.id", Equal, "123456")
      )
    )

    val insertDataA: Seq[PreparedVariable] = Seq(
      PreparedVariable.IntVariable(3),
      PreparedVariable.FloatVariable(100.1f),
      PreparedVariable.StringVariable("John Smith"),
      PreparedVariable.BoolVariable(true),
      PreparedVariable.DateVariable(Date.valueOf("1998-03-05")),
      PreparedVariable.TimeVariable(Time.valueOf("12:00:00")),
      PreparedVariable.DateTimeTzVariable(Timestamp.valueOf("2020-01-01 00:00:00.0"))
    )

    val insertDataB: Seq[PreparedVariable] = Seq(
      PreparedVariable.IntVariable(123456),
      PreparedVariable.FloatVariable(23.42f),
      PreparedVariable.StringVariable("Jane Doe"),
      PreparedVariable.BoolVariable(false),
      PreparedVariable.DateVariable(Date.valueOf("1998-03-05")),
      PreparedVariable.TimeVariable(Time.valueOf("12:00:00")),
      PreparedVariable.DateTimeTzVariable(Timestamp.valueOf("2019-02-03 02:23:50.0"))
    )

  }
}
