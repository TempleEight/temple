package temple.containers

import com.whisk.docker.{DockerCommandExecutor, DockerContainer, DockerContainerState, DockerKit, DockerReadyChecker}
import scalaj.http.Http
import temple.containers.DockerHadolintService.HadolintReadyChecker

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/** DockerHadolintService configures a docker container for hadolint */
trait DockerHadolintService extends DockerKit {

  val hadolintContainer: DockerContainer = DockerContainer(DockerHadolintService.image)
    .withPorts(DockerHadolintService.internalPort -> Some(DockerHadolintService.externalPort))
    .withReadyChecker(
      new HadolintReadyChecker().looped(5, 10.seconds),
    )

  abstract override def dockerContainers: List[DockerContainer] =
    hadolintContainer +: super.dockerContainers
}

/** DockerHadolintService encapsulates all configuration for running our custom Hadolint docker container */
object DockerHadolintService {
  val image             = "jaylees/templeeight-hadolint:1.0"
  val internalPort      = 8080
  val externalPort      = 8080
  val externalVerifyUrl = s"http://localhost:$externalPort/hadolint"

  class HadolintReadyChecker extends DockerReadyChecker {

    // Called by the ready checker, will only succeed when container has started, and is ready to receive requests
    override def apply(
      container: DockerContainerState,
    )(implicit docker: DockerCommandExecutor, ec: ExecutionContext): Future[Boolean] =
      Future(Http(s"http://localhost:${externalPort}/").asString.code == 200)
  }
}
