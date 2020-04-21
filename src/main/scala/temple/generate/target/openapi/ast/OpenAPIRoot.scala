package temple.generate.target.openapi.ast

case class OpenAPIRoot(
  name: String,
  version: String,
  description: String = "",
  auth: Option[Auth],
  services: Seq[Service],
)

trait Auth

object Auth {
  case object Email extends Auth
}

object OpenAPIRoot {

  def build(name: String, version: String, description: String = "", auth: Option[Auth] = None)(
    services: Service*,
  ): OpenAPIRoot =
    new OpenAPIRoot(name, version, description, auth, services)
}
