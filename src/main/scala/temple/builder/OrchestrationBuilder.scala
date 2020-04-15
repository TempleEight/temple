package temple.builder

import temple.ast.AbstractServiceBlock.AuthServiceBlock
import temple.ast.Metadata.Database
import temple.ast.Templefile.Ports
import temple.ast.{AbstractServiceBlock, Metadata, Templefile}
import temple.builder.project.ProjectConfig
import temple.generate.orchestration.ast.OrchestrationType.{OrchestrationRoot, Service}
import temple.generate.orchestration.kube.ast.LifecycleCommand
import temple.utils.StringUtils

object OrchestrationBuilder {

  def createServiceOrchestrationRoot(templefile: Templefile): OrchestrationRoot = {
    val services: Iterable[Service] = templefile.allServicesWithPorts map {
        case (name: String, service: AbstractServiceBlock, port: Ports) =>
          val kebabName   = StringUtils.kebabCase(name)
          val projectName = StringUtils.kebabCase(templefile.projectName)
          val dockerImage = s"${ProjectConfig.registryURL}/$projectName-$kebabName"
          val dbLanguage  = service.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase)
          val dbImage     = ProjectConfig.dockerImage(dbLanguage)
          Service(
            name = kebabName,
            image = dockerImage,
            dbImage = dbImage.toString,
            ports = Seq(
              ("api", port.service),
              ("prom", port.metrics),
            ),
            //This value assumed to be one
            replicas = 1,
            secretName = "regcred",
            appEnvVars = Seq(), //TODO: This
            dbEnvVars = dbLanguage match {
              case Database.Postgres => Seq("PGUSER" -> "postgres")
            },
            dbStorage = ProjectConfig.databaseStorage(dbLanguage, kebabName),
            dbLifecycleCommand = dbLanguage match {
              case Database.Postgres => LifecycleCommand.psqlSetup.toString
            },
            usesAuth = service != AuthServiceBlock && templefile.usesAuth,
          )
      }
    OrchestrationRoot(services.toSeq, templefile.lookupMetadata[Metadata.Metrics].nonEmpty)
  }
}
