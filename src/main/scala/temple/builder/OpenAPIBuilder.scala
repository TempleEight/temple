package temple.builder

import temple.ast.Templefile
import temple.builder.project.ProjectBuilder.endpoints
import temple.generate.target.openapi.ast.{OpenAPIRoot, Service}

object OpenAPIBuilder {

  def createOpenAPI(templefile: Templefile, version: String = "0.0.1", description: String = ""): OpenAPIRoot =
    OpenAPIRoot(
      name = templefile.projectName,
      version = version,
      description = description,
      services = templefile.providedServices.map {
        case (serviceName, block) =>
          Service(
            name = serviceName,
            operations = endpoints(block),
            attributes = block.attributes,
            structs = block.structs.map {
              case (structName, structBlock) =>
                Service.Struct(structName, endpoints(structBlock), attributes = structBlock.attributes)
            },
          )
      }.toSeq,
    )
}
