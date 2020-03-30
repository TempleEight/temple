package temple.generate.database.ast

import temple.generate.database.ast.Expression.PreparedValue

/** AST representation for assignments */
sealed case class Assignment(column: Column, expression: Expression = PreparedValue)
