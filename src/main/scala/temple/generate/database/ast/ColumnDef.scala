package temple.generate.database.ast

/** AST implementation of database column definitions for all data types supported in Templefile */
sealed case class ColumnDef(name: String, colType: ColType, constraints: Seq[ColumnConstraint] = Nil)
