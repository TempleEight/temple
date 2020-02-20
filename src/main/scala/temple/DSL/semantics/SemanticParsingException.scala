package temple.DSL.semantics

/** An exception in converting the AST into a [[temple.DSL.semantics.Templefile]] */
// TODO: include contextual information
class SemanticParsingException(str: String) extends Exception(str)
