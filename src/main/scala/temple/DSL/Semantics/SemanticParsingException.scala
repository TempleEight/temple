package temple.DSL.Semantics

/** An exception in converting the AST into a [[temple.DSL.Semantics.Templefile]] */
// TODO: include contextual information
class SemanticParsingException(str: String) extends Exception(str)
