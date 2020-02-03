package temple.containers

import java.sql.{Connection, DriverManager, ResultSet}

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/** PostgresSpec offers additional methods to test commands using Postgres database */
trait PostgresSpec extends FlatSpec with DockerTestKit with DockerPostgresService with BeforeAndAfterAll {

  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  private def execute[T](fn: Connection => T): Option[T] =
    Option(
      DriverManager.getConnection(
        DockerPostgresService.externalUrl,
        DockerPostgresService.databaseUsername,
        DockerPostgresService.databasePassword
      )
    ).map { conn =>
      try fn(conn)
      finally conn.close()
    }

  // Execute a query that is expected to return a result
  def executeWithResults(query: String): Option[ResultSet] = execute { _.createStatement().executeQuery(query) }

  // Execute a query that does not return a result
  def executeWithoutResults(query: String): Unit = execute { _.createStatement().execute(query) }
}
