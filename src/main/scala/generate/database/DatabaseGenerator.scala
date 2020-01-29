package generate.database

import generate.database.ast.Statement

trait DatabaseGenerator {
  def generate(statement: Statement): String
}
