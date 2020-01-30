package generate.database.ast

/** AST implementation of database columns for all data types supported in Templefile */
sealed case class Column(name: String, colType: Option[ColType] = None)

sealed trait ColType

object ColType {
  case object IntCol        extends ColType
  case object FloatCol      extends ColType
  case object StringCol     extends ColType
  case object BoolCol       extends ColType
  case object DateCol       extends ColType
  case object TimeCol       extends ColType
  case object DateTimeTzCol extends ColType
}
