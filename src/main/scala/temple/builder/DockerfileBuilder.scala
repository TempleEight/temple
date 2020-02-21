package temple.builder

import temple.DSL.semantics.Metadata.Database.Postgres
import temple.DSL.semantics.Metadata.{Database, ServiceLanguage}
import temple.DSL.semantics.Metadata.ServiceLanguage.Go
import temple.DSL.semantics.{Metadata, ServiceBlock}
import temple.builder.project.ProjectConfig
import temple.generate.docker.ast.{DockerfileRoot, Statement}
import temple.generate.docker.ast.Statement.{Copy, Entrypoint, Expose, From, Run, WorkDir}

/** Construct a Dockerfile from a Templefile service */
object DockerfileBuilder {

  def createServiceDockerfile(serviceName: String, service: ServiceBlock, port: Int): DockerfileRoot = {
    val language                               = service.lookupMetadata[Metadata.ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    val dockerImage: ProjectConfig.DockerImage = ProjectConfig.dockerImage(language)

    val commands: Seq[Statement] = language match {
      case ServiceLanguage.Go =>
        Seq(
          WorkDir(s"/$serviceName"),
          Copy("go.mod go.sum", "./"),
          Run("go", Seq("mod", "download")),
          Copy(".", "."),
          Copy("config.json", s"/etc/$serviceName-service"),
          Run("go", Seq("build", "-o", serviceName)),
          Entrypoint(s"./$serviceName", Seq()),
          Expose(port),
        )
      case ServiceLanguage.Scala =>
        Seq()
    }

    DockerfileRoot(From(dockerImage.image, Some(dockerImage.version)), commands)
  }
}
