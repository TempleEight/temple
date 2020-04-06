package temple.generate.server

/**
  * CreatedByAttribute encapsulates the temple defined attribute used to track which authenticated ID created a resource
  *
  * @param inputName the name of the variable used as input, e.g. "authID", present for improving semantics of generated
  *                  code
  * @param name the name of the attribute, e.g. "createdBy"
  */
case class CreatedByAttribute(inputName: String, name: String)
