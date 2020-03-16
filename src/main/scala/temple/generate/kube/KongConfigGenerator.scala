package temple.generate.kube

import temple.generate.FileSystem.{File, FileContent}
import temple.generate.kube.ast.OrchestrationType.{OrchestrationRoot, Service}
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils.indent

/**
  * Generator for kong config scripts - a series of curl commands to kong to configure service routing
  */
object KongConfigGenerator {

  private val shebang = "#!/bin/sh"

  /** Generate a kong service declaration for a Temple service */
  private def generateServiceDef(service: Service): String = {
    val urls = service.ports map {
        case (_, port) => s"--data 'url=http://${service.name}:$port/${service.name}'"
      }

    mkCode.escapedList(
      "curl -i -X POST",
      indent("--url $KONG_ADMIN/services/"),
      indent(s"--data 'name=${service.name}-service'"),
      urls.map(indent(_)),
    )
  }

  /** Generate a kong route to the service for a Temple service */
  private def generateServiceRoute(service: Service): String =
    mkCode.escapedList(
      "curl -i -X POST",
      indent(s"--url $$KONG_ADMIN/services/${service.name}-service/routes"),
      indent("--data \"hosts[]=$KONG_ENTRY\""),
      indent(s"--data 'paths[]=/api/${service.name}'"),
    )

  /** Given an auth-enabled Temple service, generate the kong auth request */
  private def generateAuthRequirement(service: Service): String =
    mkCode.escapedList(
      "curl -X POST",
      indent(s"--url $$KONG_ADMIN/services/${service.name}-service/plugins"),
      indent("--data 'name=jwt'"),
      indent("--data 'config.claims_to_verify=exp'"),
    )

  /**
    * Generates the Kong config script
    * @param orchestrationRoot the information about services in the application
    * @return A tuple of the filename to it's content
    */
  def generate(orchestrationRoot: OrchestrationRoot): (File, FileContent) = {
    val config = mkCode.doubleLines(
      this.shebang,
      orchestrationRoot.services.map(generateServiceDef),
      orchestrationRoot.services.map(generateServiceRoute),
      orchestrationRoot.services.filter(_.usesAuth).map(generateAuthRequirement),
    )
    File("kong", "configure-kong.sh") -> config
  }
}
