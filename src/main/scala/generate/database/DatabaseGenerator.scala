package generate.database

import generate.database.ast.Statement

/** DatabaseGenerator provides an interface for generating DB specific query languages from an AST */
trait DatabaseGenerator {

  /** Given a Database AST, generate a implementation specific query language */
  def generate(statement: Statement): String
}
