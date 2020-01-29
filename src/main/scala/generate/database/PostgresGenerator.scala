package generate.database
import generate.database.ast._

class PostgresGenerator extends DatabaseGenerator {

  private val sb = new StringBuilder()

  override def getText: String =
    this.sb.mkString

  override def generate(statement: Statement): Unit =
    statement match {
      case Create(tableName, columns) =>
        sb.append(s"CREATE TABLE $tableName (\n")
        columns foreach {
          case IntColumn(name)        => sb.append(s"    $name INT,\n")
          case StringColumn(name)     => sb.append(s"    $name TEXT,\n")
          case DateTimeTzColumn(name) => sb.append(s"    $name TIMESTAMPTZ,\n")
          case BoolColumn(name)       => sb.append(s"    $name BOOLEAN,\n")
          case FloatColumn(name)      => sb.append(s"    $name REAL,\n")
          case DateColumn(name)       => sb.append(s"    $name DATE,\n")
          case TimeColumn(name)       => sb.append(s"    $name TIME,\n")
        }
        sb.append(");\n")
    }

}
