package temple.containers

import java.sql.{Connection, DriverManager, ResultSet}

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import temple.generate.database.PreparedVariable

/** PostgresSpec offers additional methods to test commands using Postgres database */
trait PostgresSpec extends FlatSpec with DockerTestKit with DockerPostgresService with BeforeAndAfterAll {

  // Required to implement DockerTestKit: use the default configuration to create a docker client
  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  private def execute[T](fn: Connection => T): Option[T] =
    Option(
      DriverManager.getConnection(
        DockerPostgresService.externalUrl,
        DockerPostgresService.databaseUsername,
        DockerPostgresService.databasePassword
      )
    ) map { conn =>
      try fn(conn)
      finally conn.close()
    }

  // Execute a query that is expected to return a result
  def executeWithResults(query: String): Option[ResultSet] = execute { _.createStatement().executeQuery(query) }

  // Execute a query that does not return a result
  def executeWithoutResults(query: String): Unit = execute { _.createStatement().execute(query) }

  def executeWithoutResultsPrepared(preparedStatement: String, values: List[PreparedVariable]): Unit = execute { conn =>
    val prep = conn.prepareStatement(preparedStatement)
    values.view.zipWithIndex foreach {
      case (v, i) =>
        val variable_number = i + 1
        v match {
          case PreparedVariable.IntVariable(value)        => prep.setInt(variable_number, value)
          case PreparedVariable.BoolVariable(value)       => prep.setBoolean(variable_number, value)
          case PreparedVariable.StringVariable(value)     => prep.setString(variable_number, value)
          case PreparedVariable.FloatVariable(value)      => prep.setFloat(variable_number, value)
          case PreparedVariable.DateVariable(value)       => prep.setDate(variable_number, value)
          case PreparedVariable.TimeVariable(value)       => prep.setTime(variable_number, value)
          case PreparedVariable.DateTimeTzVariable(value) => prep.setTimestamp(variable_number, value)
        }
    }
    prep.execute()
  }

}
