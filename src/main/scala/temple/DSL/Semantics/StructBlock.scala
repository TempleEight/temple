package temple.DSL.Semantics

/** A nested struct within a service, representing a second table/domain in the same datastore */
case class StructBlock(
  attributes: Map[String, Attribute]
)
