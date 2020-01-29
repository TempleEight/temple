package generate.database

abstract class DatabaseGenerator {

  def getText: String

  def create(tableName: String): Unit

  def select(tableName: String): Unit

  def delete(tableName: String): Unit
}
