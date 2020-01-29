package generate.database

class PostgresGenerator extends DatabaseGenerator {

  private val sb = new StringBuilder()

  override def getText: String = {
    this.sb.mkString
  }

  override def create(tableName: String): Unit = {
    this.sb.append(s"CREATE TABLE $tableName\n")
  }

  override def select(tableName: String): Unit = {
    this.sb.append(s"SELECT FROM $tableName\n")
  }

  override def delete(tableName: String): Unit = {
    this.sb.append(s"DELETE FROM $tableName\n")
  }
}
