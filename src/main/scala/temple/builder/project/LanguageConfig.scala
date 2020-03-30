package temple.builder.project

import temple.ast.AttributeType

/** Defines the values used for generation in a language that do not come from the user
  * inputName the name of the variable used as input, e.g. "authID", present for improving semantics of code
  * name the name of attribute, e.g. "createdBy"
  * attributeType type of the attribute, e.g. UUID
  * */
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
