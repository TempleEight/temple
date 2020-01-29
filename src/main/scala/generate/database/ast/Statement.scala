package generate.database.ast

sealed trait Statement

case class Create(tableName: String, columns: List[Column]) extends Statement
