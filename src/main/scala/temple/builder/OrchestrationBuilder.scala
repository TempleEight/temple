package temple.builder

import temple.ast.Metadata.Database
import temple.ast.{AbstractServiceBlock, Metadata}
import temple.builder.project.ProjectConfig
import temple.generate.kube.ast.OrchestrationType.{OrchestrationRoot, Service}
import temple.generate.kube.ast.gen.LifecycleCommand
import temple.utils.StringUtils

object OrchestrationBuilder {

  def createServiceOrchestrationRoot(
    projectName: String,
    services: Seq[(String, AbstractServiceBlock, Int)],
  ): OrchestrationRoot =
    OrchestrationRoot(
      services map {
        case (name, service, port) =>
          val kebabName   = StringUtils.kebabCase(name)
          val dockerImage = s"$projectName-$kebabName"
          val dbLanguage  = service.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase)
          val dbImage     = ProjectConfig.dockerImage(dbLanguage)
          Service(
            name = kebabName,
            image = dockerImage,
            dbImage = dbImage.toString,
            ports = Seq(("api", port)),
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
            usesAuth = name != "Auth",
          )
      },
    )
}
