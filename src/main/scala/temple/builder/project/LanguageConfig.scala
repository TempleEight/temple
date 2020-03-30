package temple.builder.project

import temple.ast.AttributeType

/** Defines the values used for generation in a language that do not come from the user */
sealed trait LanguageConfig {
  val createdByInputName: String
  val createdByName: String
  val createdByAttributeType: AttributeType
}

object LanguageConfig {

  /** The implicit values used to generate Golang code that don't come from the user */
  object GoLanguageConfig extends LanguageConfig {
    override val createdByInputName: String            = "authID"
    override val createdByName: String                 = "createdBy"
    override val createdByAttributeType: AttributeType = AttributeType.UUIDType
  }
}
