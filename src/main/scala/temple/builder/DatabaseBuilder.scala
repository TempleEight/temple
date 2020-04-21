package temple.builder

import temple.ast.AbstractAttribute.{CreatedByAttribute, IDAttribute}
import temple.ast._
import temple.generate.CRUD._
import temple.generate.database.ast.ColumnConstraint.Check
import temple.generate.database.ast.Condition.{Disjunction, PreparedComparison}
import temple.generate.database.ast.Expression.PreparedValue
import temple.generate.database.ast._
import temple.utils.StringUtils.snakeCase

import scala.Option.when
import scala.collection.immutable.SortedMap

/** Construct database queries from a Templefile structure */
object DatabaseBuilder {

  private def generateMaxMinConstraints[T](name: String, max: Option[T], min: Option[T]): Seq[ColumnConstraint] = {
    val maxCondition = max.map(v => Check(name, ComparisonOperator.LessEqual, v.toString))
    val minCondition = min.map(v => Check(name, ComparisonOperator.GreaterEqual, v.toString))
    Seq(maxCondition, minCondition).flatten
  }

  private def toColDef(name: String, attribute: AbstractAttribute): ColumnDef = {
    val nonNullConstraint = Some(ColumnConstraint.NonNull)

    val primaryKeyConstraint = when(attribute == IDAttribute) { ColumnConstraint.PrimaryKey }

    val valueConstraints = attribute.valueAnnotations.flatMap {
        case Annotation.Unique => Some(ColumnConstraint.Unique)
      } ++ nonNullConstraint ++ primaryKeyConstraint

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

  def conditionsToDisjunction(terms: Iterable[Condition]): Option[Condition] = when(terms.nonEmpty) {
    if (terms.sizeIs == 1) terms.head else Disjunction(terms.head, conditionsToDisjunction(terms.tail).get)
  }

  def buildQueries(
    serviceName: String,
    attributes: Map[String, AbstractAttribute],
    endpoints: Set[CRUD],
    isStruct: Boolean,
    readable: Metadata.Readable = Metadata.Readable.This,
    selectionAttribute: String = "id",
  ): SortedMap[CRUD, Statement] = {
    val tableName = snakeCase(serviceName)
    val columns   = attributes.keys.map(att => Column(snakeCase(att))).toSeq
    val providedColumns =
      attributes
        .filter { case (_, attr) => attr != IDAttribute && attr != CreatedByAttribute }
        .keys
        .map(att => Column(snakeCase(att)))
        .toSeq
    endpoints
      .map {
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
            assignments = providedColumns.map(Assignment(_, PreparedValue)),
            condition = Some(PreparedComparison(selectionAttribute, ComparisonOperator.Equal)),
            returnColumns = columns,
          )
        case Delete =>
          Delete -> Statement.Delete(
            tableName,
            condition = Some(PreparedComparison(selectionAttribute, ComparisonOperator.Equal)),
          )
        case List =>
          List -> Statement.Read(
            tableName,
            columns = columns,
            condition = conditionsToDisjunction(
              when(readable == Metadata.Readable.This) {
                PreparedComparison("created_by", ComparisonOperator.Equal)
              } ++ when(isStruct) {
                PreparedComparison("parent_id", ComparisonOperator.Equal)
              },
            ),
          )
        case Identify =>
          Identify -> Statement.Read(
            tableName,
            columns = columns,
            condition = Some(PreparedComparison(selectionAttribute, ComparisonOperator.Equal)),
          )
      }
      .to(SortedMap)
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
      case (tableName, structBlock) =>
        val columns = structBlock.storedAttributes
          .map { case (name, attribute) => toColDef(snakeCase(name), attribute) }
        Statement.Create(snakeCase(tableName), columns.toSeq)
    }
  }.toSeq
}
