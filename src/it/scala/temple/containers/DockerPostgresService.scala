package temple.containers

import java.sql.DriverManager

import com.whisk.docker.{DockerCommandExecutor, DockerContainer, DockerContainerState, DockerKit, DockerReadyChecker}
import temple.containers.DockerPostgresService.PostgresReadyChecker

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/** DockerPostgresService encapsulates all configuration for running a postgres docker container */
object DockerPostgresService {
  val image            = "postgres:9.6"
  val databaseUsername = "postgres"
  val databasePassword = ""
  val databaseName     = "temple_test"
  val internalPort     = 5432
  val externalPort     = 44444
  val externalUrl      = s"jdbc:postgresql://localhost:$externalPort/$databaseName"

  class PostgresReadyChecker extends DockerReadyChecker {

    // Called by the ready checker, will only succeed when container has started
    override def apply(
      container: DockerContainerState
    )(implicit docker: DockerCommandExecutor, ec: ExecutionContext): Future[Boolean] = {
      val url = s"jdbc:postgresql://${docker.host}:$externalPort/"
      Future {
        Option(DriverManager.getConnection(url, databaseUsername, databasePassword)) map { conn =>
          try conn.createStatement().execute(s"CREATE DATABASE $databaseName")
          finally conn.close()
        }
        true
      }
    }
  }
}

/** DockerPostgresService configures a docker container for Postgres */
trait DockerPostgresService extends DockerKit {

  val postgresContainer: DockerContainer = DockerContainer(DockerPostgresService.image)
    .withPorts(DockerPostgresService.internalPort -> Some(DockerPostgresService.externalPort))
    .withEnv(
      s"POSTGRES_USER=${DockerPostgresService.databaseUsername}",
      s"POSTGRES_PASSWORD=${DockerPostgresService.databasePassword}"
    )
    .withReadyChecker(
      new PostgresReadyChecker().looped(5, 10.seconds)
    )

  abstract override def dockerContainers: List[DockerContainer] =
    postgresContainer +: super.dockerContainers
}
