package temple.generate.database.ast

/** AST representation for assignments */
sealed case class Assignment(column: Column, expression: Expression)
