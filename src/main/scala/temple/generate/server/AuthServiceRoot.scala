package temple.generate.server

import temple.ast.Metadata.ServiceAuth

case class AuthServiceRoot(module: String, port: Int, authAttribute: AuthAttribute, idAttribute: IDAttribute)
