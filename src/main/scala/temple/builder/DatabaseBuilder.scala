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

    val (colType, constraints) = attribute.attributeType match {
      case AttributeType.BoolType     => (ColType.BoolCol, valueConstraints)
      case AttributeType.DateType     => (ColType.DateCol, valueConstraints)
      case AttributeType.DateTimeType => (ColType.DateTimeTzCol, valueConstraints)
      case AttributeType.TimeType     => (ColType.TimeCol, valueConstraints)
      case AttributeType.ForeignKey   => (ColType.IntCol(4), valueConstraints)
      case AttributeType.BlobType(_)  => (ColType.BlobCol, valueConstraints)
      case AttributeType.IntType(max, min, precision) =>
        (ColType.IntCol(precision), valueConstraints ++ generateMaxMinConstraints(name, max, min))
      case AttributeType.FloatType(max, min, precision) =>
        (ColType.FloatCol(precision), valueConstraints ++ generateMaxMinConstraints(name, max, min))
      case AttributeType.StringType(max, min) =>
        val colType = if (max.isDefined) ColType.BoundedStringCol(max.get) else ColType.StringCol
        (colType, valueConstraints ++ generateMaxMinConstraints(s"length($name)", None, min))
    }
    ColumnDef(name, colType, constraints)
  }

  /**
    * Converts a service block to an associated list of database table create statement
    * @param serviceName The service name
    * @param service The ServiceBlock to generate
    * @return the associated create statement
    */
  def createServiceTables(serviceName: String, service: ServiceBlock): Seq[Statement.Create] = {
    // Create a list of all tables to be created, using top level attributes and nested structs
    val allTables = service.structs.map(s => (s._1, s._2.attributes)).toSeq :+ (serviceName, service.attributes)

    // Generate the create statement for each table
    allTables
      .map {
        case (tableName, attributes) =>
          val columns = attributes.map(a => toColDef(a._1, a._2))
          Statement.Create(tableName, columns.toSeq)
      }
  }
}
