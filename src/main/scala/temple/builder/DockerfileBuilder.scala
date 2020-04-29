package temple.builder

import temple.ast.Metadata.Provider
import temple.ast.{AbstractServiceBlock, Metadata}
import temple.builder.project.ProjectConfig
import temple.generate.docker.ast.Statement._
import temple.generate.docker.ast.{DockerfileRoot, Statement}

/** Construct a Dockerfile from a Templefile service */
object DockerfileBuilder {

  def createServiceDockerfile(
    serviceName: String,
    service: AbstractServiceBlock,
    port: Int,
    provider: Option[Provider],
  ): DockerfileRoot = {
    val language    = service.lookupMetadata[Metadata.ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    val dockerImage = ProjectConfig.dockerImage(language)

    // If using DockerCompose and this is the auth service, change the exec
    val execStatements: Option[Seq[Statement]] = service match {
      case AbstractServiceBlock.AuthServiceBlock =>
        provider.collect {
          case Provider.DockerCompose =>
            Seq(
              Run("apk", Seq("add", "curl")),
              Run("chmod", Seq("+x", "wait-for-kong.sh")),
              Cmd("./wait-for-kong.sh", Seq("kong:8001", "--", "./auth")),
            )
        }
      case _ => None
    }

    val commands: Seq[Statement] = language match {
      case Metadata.ServiceLanguage.Go =>
        Seq(
          WorkDir(s"/$serviceName"),
          Copy("go.mod", "./"),
          Run("go", Seq("mod", "download")),
          Copy(".", "."),
          Copy("config.json", s"/etc/$serviceName-service/"),
          Run("go", Seq("build", "-o", serviceName)),
        ) ++ execStatements.getOrElse(
          Seq(Entrypoint(s"./$serviceName", Seq())),
        ) :+ Expose(port)
    }

    DockerfileRoot(From(dockerImage.image, Some(dockerImage.version)), commands)
  }
}
