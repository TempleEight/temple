package temple.DSL.semantics

/** An exception in converting the AST into a [[temple.ast.Templefile]] */
class SemanticParsingException(str: String) extends Exception(str)
