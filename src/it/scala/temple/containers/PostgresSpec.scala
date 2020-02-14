package temple.containers

import java.sql.{Connection, DriverManager, ResultSet}

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import temple.generate.database.PreparedVariable
import temple.generate.database.PreparedVariable._

/** PostgresSpec offers additional methods to test commands using Postgres database */
trait PostgresSpec extends FlatSpec with DockerTestKit with DockerPostgresService with BeforeAndAfterAll {

  // Required to implement DockerTestKit: use the default configuration to create a docker client
  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  private def execute[T](fn: Connection => T): Option[T] =
    Option(
      DriverManager.getConnection(
        DockerPostgresService.externalUrl,
        DockerPostgresService.databaseUsername,
        DockerPostgresService.databasePassword,
      ),
    ) map { conn =>
      try fn(conn)
      finally conn.close()
    }

  // Execute a query that is expected to return a result
  def executeWithResults(query: String): Option[ResultSet] = execute { _.createStatement().executeQuery(query) }

  // Execute a query that does not return a result
  def executeWithoutResults(query: String): Unit = execute { _.createStatement().execute(query) }

  /** Execute a query that does not return a result, but takes a sequence of prepared values that need to be set */
  def executePreparedWithoutResults(preparedStatement: String, values: Seq[PreparedVariable]): Unit = execute { conn =>
    val prep = conn.prepareStatement(preparedStatement)
    values.view.zip(Iterator from 1) foreach {
      case (v, i) =>
        v match {
          case IntVariable(value)        => prep.setInt(i, value)
          case BoolVariable(value)       => prep.setBoolean(i, value)
          case StringVariable(value)     => prep.setString(i, value)
          case FloatVariable(value)      => prep.setFloat(i, value)
          case DateVariable(value)       => prep.setDate(i, value)
          case TimeVariable(value)       => prep.setTime(i, value)
          case DateTimeTzVariable(value) => prep.setTimestamp(i, value)
          case BlobVariable(value)       => prep.setBytes(i, value)
        }
    }
    prep.execute()
  }

}
