package temple.generate.server

import temple.ast.AttributeType

sealed trait CreatedByAttribute

object CreatedByAttribute {

  /**
    * None encapsulates the case where no Temple defined createdBy field exists, e.g. in the case of an Auth block
    */
  case object None extends CreatedByAttribute

  sealed trait Enumerating extends CreatedByAttribute {
    def inputName: String
    def name: String
    def attributeType: AttributeType
  }

  /**
    * EnumerateByThis encapsulates the case where a Temple defined createdBy field exists and is used to enumerate the
    * List operation
    *
    * @param inputName the name of the variable used as input, e.g. "authID", present for improving semantics of code
    * @param name the name of attribute, e.g. "createdBy"
    * @param attributeType type of the attribute, e.g. UUID
    */
  case class EnumerateByCreator(inputName: String, name: String, attributeType: AttributeType) extends Enumerating

  /**
    * EnumerateByAll encapsulates the case where a Temple defined createdBy field exists, but is not used to enumerate
    * the List operation
    *
    * @param inputName the name of the variable used as input, e.g. "authID", present for improving semantics of code
    * @param name the name of attribute, e.g. "createdBy"
    * @param attributeType type of the attribute, e.g. UUID
    */
  case class EnumerateByAll(inputName: String, name: String, attributeType: AttributeType) extends Enumerating
}
