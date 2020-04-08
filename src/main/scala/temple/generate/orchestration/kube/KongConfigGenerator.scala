package temple.generate.orchestration.kube

import temple.generate.FileSystem.{File, FileContent}
import temple.generate.orchestration.ast.OrchestrationType.{OrchestrationRoot, Service}
import temple.generate.utils.CodeTerm.mkCode

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

    mkCode.shellLines(
      "curl -X POST",
      "--url $KONG_ADMIN/services/",
      s"--data 'name=${service.name}-service'",
      urls,
    )
  }

  /** Generate a kong route to the service for a Temple service */
  private def generateServiceRoute(service: Service): String =
    mkCode.shellLines(
      "curl -X POST",
      s"--url $$KONG_ADMIN/services/${service.name}-service/routes",
      """--data "hosts[]=$KONG_ENTRY"""",
      s"--data 'paths[]=/api/${service.name}'",
    )

  /** Given an auth-enabled Temple service, generate the kong auth request */
  private def generateAuthRequirement(service: Service): String =
    mkCode.shellLines(
      "curl -X POST",
      s"--url $$KONG_ADMIN/services/${service.name}-service/plugins",
      "--data 'name=jwt'",
      "--data 'config.claims_to_verify=exp'",
    )

  /**
    * Generates the Kong config script
    * @param orchestrationRoot the information about services in the application
    * @return A tuple of the filename to it's content
    */
  def generate(orchestrationRoot: OrchestrationRoot): (File, FileContent) = {
    val config = mkCode.doubleLines(
        shebang,
        orchestrationRoot.services.map(generateServiceDef),
        orchestrationRoot.services.map(generateServiceRoute),
        orchestrationRoot.services.filter(_.usesAuth).map(generateAuthRequirement),
      ) + "\n"
    File("kong", "configure-kong.sh") -> config
  }
}
