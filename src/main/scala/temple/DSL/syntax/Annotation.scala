package temple.DSL.syntax

/** The annotation of a struct’s attribute */
sealed case class Annotation(key: String) { override def toString: String = s"@$key" }
