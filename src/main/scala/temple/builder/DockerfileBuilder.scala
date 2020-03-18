package temple.builder

import temple.ast.{Metadata, ServiceBlock}
import temple.builder.project.ProjectConfig
import temple.generate.docker.ast.Statement._
import temple.generate.docker.ast.{DockerfileRoot, Statement}

/** Construct a Dockerfile from a Templefile service */
object DockerfileBuilder {

  def createServiceDockerfile(serviceName: String, service: ServiceBlock, port: Int): DockerfileRoot = {
    val language    = service.lookupMetadata[Metadata.ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    val dockerImage = ProjectConfig.dockerImage(language)

    val commands: Seq[Statement] = language match {
      case Metadata.ServiceLanguage.Go =>
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
    }

    DockerfileRoot(From(dockerImage.image, Some(dockerImage.version)), commands)
  }
}
