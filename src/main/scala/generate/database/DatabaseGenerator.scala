package generate.database

import generate.database.ast.Statement

abstract class DatabaseGenerator {

  def getText: String

  def generate(statement: Statement): Unit
}
