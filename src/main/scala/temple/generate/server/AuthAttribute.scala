package temple.generate.server

import temple.ast.AttributeType
import temple.ast.Metadata.AuthMethod

case class AuthAttribute(authMethod: AuthMethod, attributeType: AttributeType)
