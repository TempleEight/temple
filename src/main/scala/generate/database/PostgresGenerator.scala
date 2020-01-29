package generate.database

import generate.database.ast._

object PostgresGenerator extends DatabaseGenerator {

  override def generate(statement: Statement): String = {
    val sb = new StringBuilder()
    statement match {
      case Create(tableName, columns) =>
        sb.append(s"CREATE TABLE $tableName (\n")
        columns foreach {
          case IntColumn(name)        => sb.append(s"    $name INT,\n")
          case FloatColumn(name)      => sb.append(s"    $name REAL,\n")
          case StringColumn(name)     => sb.append(s"    $name TEXT,\n")
          case BoolColumn(name)       => sb.append(s"    $name BOOLEAN,\n")
          case DateColumn(name)       => sb.append(s"    $name DATE,\n")
          case TimeColumn(name)       => sb.append(s"    $name TIME,\n")
          case DateTimeTzColumn(name) => sb.append(s"    $name TIMESTAMPTZ,\n")
        }
        sb.append(");\n")
    }
    sb.mkString
  }

}
