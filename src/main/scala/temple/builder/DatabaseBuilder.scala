package temple.builder

import temple.ast.Annotation.Nullable
import temple.ast.{AbstractServiceBlock, Annotation, Attribute, AttributeType}
import temple.generate.CRUD.{CRUD, Create, Delete, List, Read, Update}
import temple.generate.database.ast.ColumnConstraint.Check
import temple.generate.database.ast.Condition.PreparedComparison
import temple.generate.database.ast.Expression.PreparedValue
import temple.generate.database.ast._
import temple.generate.server.CreatedByAttribute
import temple.utils.StringUtils

import scala.collection.immutable.ListMap

/** Construct database queries from a Templefile structure */
object DatabaseBuilder {

  private def generateMaxMinConstraints[T](name: String, max: Option[T], min: Option[T]): Seq[ColumnConstraint] = {
    val maxCondition = max.map(v => Check(name, ComparisonOperator.LessEqual, v.toString))
    val minCondition = min.map(v => Check(name, ComparisonOperator.GreaterEqual, v.toString))
    Seq(maxCondition, minCondition).flatten
  }

  private def toColDef(name: String, attribute: Attribute): ColumnDef = {
    val nonNullConstraint = Option.when(!attribute.valueAnnotations.contains(Nullable)) { ColumnConstraint.NonNull }

    val valueConstraints = attribute.valueAnnotations.flatMap {
        case Annotation.Unique   => Some(ColumnConstraint.Unique)
        case Annotation.Nullable => None
      } ++ nonNullConstraint

    val (colType, typeConstraints) = attribute.attributeType match {
      case AttributeType.BoolType      => (ColType.BoolCol, Nil)
      case AttributeType.DateType      => (ColType.DateCol, Nil)
      case AttributeType.DateTimeType  => (ColType.DateTimeTzCol, Nil)
      case AttributeType.TimeType      => (ColType.TimeCol, Nil)
      case AttributeType.ForeignKey(_) => (ColType.UUIDCol, Nil)
      case AttributeType.UUIDType      => (ColType.UUIDCol, Nil)
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
    ColumnDef(name, colType, typeConstraints ++ valueConstraints)
  }

  def buildQuery(
    serviceName: String,
    attributes: Map[String, Attribute],
    endpoints: Set[CRUD],
    createdByAttribute: Option[CreatedByAttribute],
    selectionAttribute: String = "id",
  ): ListMap[CRUD, Statement] = {
    val tableName = StringUtils.snakeCase(serviceName)
    val columns   = attributes.keys.map(Column).toSeq
    ListMap.from(
      endpoints.map {
        case Create =>
          Create -> Statement.Insert(
            tableName,
            assignment = columns.map(Assignment(_, PreparedValue)),
            returnColumns = columns,
          )
        case Read =>
          Read -> Statement.Read(
            tableName,
            columns = columns,
            condition = Some(PreparedComparison(selectionAttribute, ComparisonOperator.Equal)),
          )
        case Update =>
          Update -> Statement.Update(
            tableName,
            assignments = columns.map(Assignment(_, PreparedValue)),
            condition = Some(PreparedComparison(selectionAttribute, ComparisonOperator.Equal)),
            returnColumns = columns,
          )
        case Delete =>
          Delete -> Statement.Delete(
            tableName,
            condition = Some(PreparedComparison(selectionAttribute, ComparisonOperator.Equal)),
          )
        case List =>
          createdByAttribute match {
            case Some(CreatedByAttribute(_, _, true)) =>
              List -> Statement.Read(
                tableName,
                columns = columns,
                condition = Some(PreparedComparison("created_by", ComparisonOperator.Equal)),
              )
            case _ =>
              List -> Statement.Read(
                tableName,
                columns = columns,
              )
          }
      },
    )
  }

  /**
    * Converts a service block to an associated list of database table create statement
    *
    * @param serviceName The service name
    * @param service     The ServiceBlock to generate
    * @return the associated create statement
    */
  def createServiceTables(serviceName: String, service: AbstractServiceBlock): Seq[Statement.Create] = {
    service.structIterator(serviceName).map {
      case (tableName, attributes) =>
        val columns = attributes.map { case (name, attributes) => toColDef(name, attributes) }
        Statement.Create(StringUtils.snakeCase(tableName), columns.toSeq)
    }
  }.toSeq
}
