package temple.generate.server

import temple.ast.AttributeType
import temple.ast.Metadata.ServiceAuth

case class AuthAttribute(authType: ServiceAuth, attributeType: AttributeType)
