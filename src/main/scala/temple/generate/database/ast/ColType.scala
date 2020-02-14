package temple.generate.database.ast

sealed trait ColType

object ColType {
  case class IntCol(precision: Short)   extends ColType
  case class FloatCol(precision: Short) extends ColType
  case object StringCol                 extends ColType
  case object BoolCol                   extends ColType
  case object DateCol                   extends ColType
  case object TimeCol                   extends ColType
  case object DateTimeTzCol             extends ColType
  case object BlobCol                   extends ColType
}
