package temple.generate.database.ast

/** AST implementation of all basic database statements supported in Templefile */
sealed trait Statement

object Statement {
  case class Create(tableName: String, columns: Seq[ColumnDef])                                 extends Statement
  case class Read(tableName: String, columns: Seq[Column], condition: Option[Condition] = None) extends Statement

  case class Insert(tableName: String, assignment: Seq[Assignment], returnColumns: Seq[Column] = Seq())
      extends Statement

  case class Update(
    tableName: String,
    assignments: Seq[Assignment],
    condition: Option[Condition] = None,
    returnColumns: Seq[Column] = Seq(),
  ) extends Statement
  case class Delete(tableName: String, condition: Option[Condition] = None) extends Statement
  case class Drop(tableName: String, ifExists: Boolean = true)              extends Statement
}
