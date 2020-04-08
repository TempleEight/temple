package temple.containers

import com.whisk.docker.{DockerCommandExecutor, DockerContainer, DockerContainerState, DockerKit, DockerReadyChecker}
import org.scalatest.FlatSpec
import scalaj.http.Http

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/** DockerShell2HttpService configures a docker container for our custom Shell2Http container */
abstract class DockerShell2HttpService(externalPort: Int) extends FlatSpec with DockerKit {
  val image             = "jaylees/templeeight-shell2http:1.4.2"
  val internalPort      = 8080
  val hadolintVerifyUrl = s"http://localhost:$externalPort/hadolint"
  val golangVerifyUrl   = s"http://localhost:$externalPort/go"
  val swaggerVerifyUrl  = s"http://localhost:$externalPort/swagger"
  val kubeVerifyUrl     = s"http://localhost:$externalPort/kube"

  val shell2HttpContainer: DockerContainer = DockerContainer(image)
    .withPorts(internalPort -> Some(externalPort))
    .withReadyChecker(
      new Shell2HttpReadyChecker().looped(5, 10.seconds),
    )

  private class Shell2HttpReadyChecker extends DockerReadyChecker {

    // Called by the ready checker, will only succeed when container has started, and is ready to receive requests
    override def apply(
      container: DockerContainerState,
    )(implicit docker: DockerCommandExecutor, ec: ExecutionContext): Future[Boolean] =
      Future(Http(s"http://localhost:${externalPort}/").asString.code == 200)(ec)
  }

  override def dockerContainers: List[DockerContainer] =
    shell2HttpContainer +: super.dockerContainers
}
