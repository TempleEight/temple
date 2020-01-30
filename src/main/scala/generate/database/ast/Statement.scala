package generate.database.ast

/** AST implementation of all basic database statements supported in Templefile */
sealed trait Statement

case class Create(tableName: String, columns: List[Column]) extends Statement
case class Read(columns: List[Column], tableName: String)   extends Statement
