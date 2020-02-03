package temple.generate.database.ast

/** AST implementation of all basic database statements supported in Templefile */
sealed trait Statement

object Statement {
  case class Create(tableName: String, columns: List[ColumnDef])                                 extends Statement
  case class Read(tableName: String, columns: List[Column], condition: Option[Condition] = None) extends Statement
  case class Insert(tableName: String, columns: List[Column])                                    extends Statement
  case class Delete(tableName: String, condition: Option[Condition] = None)                      extends Statement
}
