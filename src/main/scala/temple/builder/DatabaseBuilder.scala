package temple.builder

import temple.DSL.semantics.{Annotation, Attribute, AttributeType, ServiceBlock}
import temple.generate.database.ast.ColumnConstraint.Check
import temple.generate.database.ast._

/** Construct database queries from a Templefile structure */
object DatabaseBuilder {

  private def generateMaxMinConstraints[T](name: String, max: Option[T], min: Option[T]): Seq[ColumnConstraint] = {
    val maxCondition = max.map(v => Check(name, ComparisonOperator.LessEqual, v.toString))
    val minCondition = min.map(v => Check(name, ComparisonOperator.GreaterEqual, v.toString))
    Seq(maxCondition, minCondition).flatten
  }

  private def toColDef(name: String, attribute: Attribute): ColumnDef = {
    val valueConstraints = attribute.valueAnnotations.map {
      case Annotation.Unique => ColumnConstraint.Unique
    }.toSeq

    val (colType, typeConstraints) = attribute.attributeType match {
      case AttributeType.BoolType     => (ColType.BoolCol, Nil)
      case AttributeType.DateType     => (ColType.DateCol, Nil)
      case AttributeType.DateTimeType => (ColType.DateTimeTzCol, Nil)
      case AttributeType.TimeType     => (ColType.TimeCol, Nil)
      case AttributeType.ForeignKey   => (ColType.IntCol(4), Nil)
      case AttributeType.BlobType(max) =>
        (ColType.BlobCol, generateMaxMinConstraints(s"octet_length($name)", max, None))
      case AttributeType.IntType(max, min, precision) =>
        (ColType.IntCol(precision), generateMaxMinConstraints(name, max, min))
      case AttributeType.FloatType(max, min, precision) =>
        (ColType.FloatCol(precision), generateMaxMinConstraints(name, max, min))
      case AttributeType.StringType(max, min) =>
        val colType = if (max.isDefined) ColType.BoundedStringCol(max.get) else ColType.StringCol
        (colType, generateMaxMinConstraints(s"length($name)", None, min))
    }
    ColumnDef(name, colType, valueConstraints ++ typeConstraints)
  }

  /**
    * Converts a service block to an associated list of database table create statement
    * @param serviceName The service name
    * @param service The ServiceBlock to generate
    * @return the associated create statement
    */
  def createServiceTables(serviceName: String, service: ServiceBlock): Seq[Statement.Create] = {
    service.structIterator(serviceName).map {
      case (tableName, attributes) =>
        val columns = attributes.map { case (name, attributes) => toColDef(name, attributes) }
        Statement.Create(tableName, columns.toSeq)
    }
  }.toSeq
}
