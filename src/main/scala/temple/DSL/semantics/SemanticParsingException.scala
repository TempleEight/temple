package temple.DSL.semantics

import temple.ast.Templefile

/** An exception in converting the AST into a [[temple.ast.Templefile]] */
// TODO: include contextual information
class SemanticParsingException(str: String) extends Exception(str)
