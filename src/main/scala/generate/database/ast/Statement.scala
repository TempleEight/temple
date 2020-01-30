package generate.database.ast

/** AST implementation of all basic database statements supported in Templefile */
sealed trait Statement

case class Create(tableName: String, columns: List[ColumnDef]) extends Statement
case class Read(tableName: String, columns: List[Column])      extends Statement
