package temple.builder

import temple.DSL.semantics.Metadata.Database
import temple.DSL.semantics.{Metadata, ServiceBlock}
import temple.builder.project.ProjectConfig
import temple.generate.kube.ast.OrchestrationType.{OrchestrationRoot, Service}
import temple.generate.kube.ast.gen.LifecycleCommand

object KubernetesBuilder {

  def createServiceKubeFiles(projectName: String, services: Seq[(String, ServiceBlock, Int)]): OrchestrationRoot =
    OrchestrationRoot(
      services map {
        case (name, service, port) =>
          val dockerImage = s"temple-$projectName-$name"
          val dbLanguage  = service.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase)
          val dbImage     = ProjectConfig.dockerImage(dbLanguage)
          Service(
            name = name,
            image = dockerImage.toString,
            dbImage = dbImage.toString,
            ports = Seq(("api", port)),
            //This value assumed to be one
            replicas = 1,
            secretName = "regcred",
            appEnvVars = Seq(), //TODO: This
            dbEnvVars = dbLanguage match {
              case Database.Postgres => Seq("PGUSER" -> "postgres")
            },
            dbStorage = ProjectConfig.databaseStorage(dbLanguage, name),
            dbLifecycleCommand = dbLanguage match {
              case Database.Postgres => LifecycleCommand.psqlSetup.toString
            },
            usesAuth = true,
          )
      },
    )
}
