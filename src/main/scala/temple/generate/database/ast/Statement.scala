package temple.generate.database.ast

/** AST implementation of all basic database statements supported in Templefile */
sealed trait Statement

object Statement {
  case class Create(tableName: String, columns: List[ColumnDef])                         extends Statement
  case class Read(tableName: String, columns: List[Column], modifiers: List[Modifier])   extends Statement
  case class Insert(tableName: String, columns: List[Column], modifiers: List[Modifier]) extends Statement
}
