package temple.generate.database

import temple.generate.database.ast.Statement

/** DatabaseGenerator provides an interface for generating DB specific query languages from an AST */
trait DatabaseGenerator[A <: DatabaseContext] {

  /** Given a Database AST, generate a implementation specific query language */
  def generate(statement: Statement)(implicit context: A): String
}
