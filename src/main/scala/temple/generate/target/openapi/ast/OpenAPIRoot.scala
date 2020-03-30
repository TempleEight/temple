package temple.generate.target.openapi.ast

case class OpenAPIRoot(name: String, version: String, description: String = "", services: Seq[Service])

object OpenAPIRoot {

  def build(name: String, version: String, description: String = "")(services: Service*): OpenAPIRoot =
    new OpenAPIRoot(name, version, description, services)
}
