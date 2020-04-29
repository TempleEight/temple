package temple.test

import java.io.IOException
import java.net.{ConnectException, HttpURLConnection}

import scalaj.http.Http
import temple.ast.Metadata.{AuthMethod, Provider}
import temple.ast.{Metadata, Templefile}
import temple.test.internal.{AuthServiceTest, CRUDServiceTest, ProjectConfig}
import io.circe.parser.parse

import scala.collection.mutable
import scala.sys.process._

object ProjectTester {

  class EnvironmentVariableNotSetException(name: String)
      extends RuntimeException(s"The environment variable $name was required, but not found")

  /** Execute a command, returning the stdout */
  private def exec(command: String): String = {
    // Collect output lines
    val result = mutable.Buffer[Either[String, String]]()
    // Run the command, with a custom handler for receiving output
    val code = s"sh -c '$command'".!(new ProcessLogger {
      override def out(s: => String): Unit = result += Right(s)
      override def err(s: => String): Unit = result += Left(s)
      override def buffer[T](f: => T): T   = f
    })
    // If there’s an error, print the log and throw an error; otherwise return the stdout log
    if (code != 0) {
      result.foreach(_.fold(stderr.println, println))
      throw new RuntimeException(s"Nonzero exit code ($code) to `$command`")
    } else {
      result.iterator.flatMap(_.toSeq).mkString("\n")
    }
  }

  /** Configure Kong using the generated script, waiting until this is successful */
  private def configureKong(
    config: ProjectConfig,
    generatedPath: String,
  ): Unit = {
    println("🦍 Configuring Kong")
    var configuredKong = false
    while (!configuredKong) {
      exec(
        s"KONG_ADMIN=${config.kongAdminURL} KONG_ENTRY=${config.baseIP} sh $generatedPath/kong/configure-kong.sh",
      )

      try {
        // TODO: this doesn't error very well
        val routesResponse = Http(s"${config.kongAdminURL}/routes").method("GET").asString
        val jsonObject     = parse(routesResponse.body).toOption.flatMap(json => json.asObject)
        val kongArray      = jsonObject.flatMap(json => json.apply("data")).flatMap(data => data.asArray)
        // Kong is configured if the response array is non empty
        configuredKong = kongArray.fold(false)(_.nonEmpty)
        if (!configuredKong) {
          println("Kong wasn't configured correctly - trying again")
        }
        // Sleep regardless so Kong has time to update with the routes
        exec("sleep 5")
      } catch {
        case _: IOException => // Keep trying
      }
    }
  }

  private def getConfig(templefile: Templefile): ProjectConfig = {
    val provider = templefile
      .lookupMetadata[Metadata.Provider]
      .getOrElse { throw new RuntimeException("Could not find deployment information") }

    provider match {
      case Provider.Kubernetes =>
        // Grab Kong's URL for future requests
        val Array(baseURL, kongAdmin, _*) = exec("minikube service kong --url").split("\n")
        val baseIP                        = baseURL.replaceAll("http[s]*://", "")
        ProjectConfig(baseIP, kongAdmin)
      case Provider.DockerCompose =>
        ProjectConfig("localhost:8000", "http://localhost:8001")
    }
  }

  /** Configure the infrastructure for testing */
  private def performSetup(templefile: Templefile, generatedPath: String): ProjectConfig = {
    val provider = templefile
      .lookupMetadata[Metadata.Provider]
      .getOrElse { throw new RuntimeException("Could not find deployment information") }

    provider match {
      case Provider.Kubernetes =>
        println("🍪 Spinning up Kubernetes infrastructure...")
        // Require certain env vars to be set
        Seq("REG_URL", "REG_USERNAME", "REG_PASSWORD", "REG_EMAIL").foreach { envVar =>
          if (System.getenv(envVar) == null) throw new EnvironmentVariableNotSetException(envVar)
        }
        exec(s"sh $generatedPath/deploy.sh")

      case Provider.DockerCompose =>
        println("🐳 Spinning up Docker Compose infrastructure...")
        exec("docker volume prune -f && docker network prune -f")
        exec(s"cd $generatedPath && docker-compose up --build -d")

        var successfullyStarted = false
        while (!successfullyStarted) {
          // If the service uses auth, wait for that service to respond, otherwise wait for kong to be available
          try {
            successfullyStarted =
              if (templefile.usesAuth)
                Http("http://localhost:1024/auth/login")
                  .method("POST")
                  .asString
                  .code == HttpURLConnection.HTTP_BAD_REQUEST
              else Http("http://localhost:8001/status").asString.code == HttpURLConnection.HTTP_OK
          } catch {
            case _: IOException => // Keep trying
          }
          if (!successfullyStarted) exec("sleep 5")
        }
    }
    val config = getConfig(templefile)
    configureKong(config, generatedPath)
    config
  }

  /** Gracefully shutdown the infrastructure */
  private def performShutdown(templefile: Templefile, generatedPath: String): Unit = {
    val provider = templefile
      .lookupMetadata[Metadata.Provider]
      .getOrElse(throw new RuntimeException("Could not find deployment information"))

    provider match {
      case Provider.Kubernetes =>
        println(s"💀 Shutting down Kubernetes infrastructure...")
        exec("kubectl drain minikube && minikube delete")
      case Provider.DockerCompose =>
        println(s"💀 Shutting down Docker Compose infrastructure...")
        exec(s"cd $generatedPath && docker-compose down -v 2>&1")
    }
  }

  /** Execute the tests on each generated service */
  private def performTests(templefile: Templefile, url: String): Unit = {
    val authMethod = templefile.lookupMetadata[AuthMethod]
    var anyFailed  = false
    authMethod.foreach { auth =>
      anyFailed = AuthServiceTest.test(auth, url) || anyFailed
    }
    templefile.providedServices.foreach {
      case (name, block) =>
        anyFailed = CRUDServiceTest
            .test(name, block, templefile.providedServices, url, templefile.usesAuth) || anyFailed
    }

    // Propagate exception up so that the exit code is relevant
    if (anyFailed) throw new RuntimeException("😢 Looks like a test didn't go as planned")
    else println("🎉 Everything passed")
  }

  /**
    * Perform a full endpoint test on a generated project, by querying each endpoint and validating responses
    * This assumes that the infrastructure is already running
    * @param templefile The parsed & validated templefile
    * @param generatedPath The root of the project where the generated code resides
    */
  def testOnly(templefile: Templefile, generatedPath: String): Unit = {
    val config = getConfig(templefile)
    try {
      // Check we can actually connect to the URL
      Http(s"http://${config.baseIP}").asString
      performTests(templefile, config.baseIP)
    } catch {
      case e: ConnectException =>
        println(s"😢 Could not connect to ${config.baseIP}, is the project running?")
        throw e
    }
  }

  /**
    * Perform a full endpoint test on a generated project, by querying each endpoint and validating responses
    * This involves deploying the service locally, performing the tests and then cleaning up
    * @param templefile The parsed & validated templefile
    * @param generatedPath The root of the project where the generated code resides
    */
  def test(templefile: Templefile, generatedPath: String): Unit =
    try {
      val config = performSetup(templefile, generatedPath)
      performTests(templefile, config.baseIP)
    } finally {
      performShutdown(templefile, generatedPath)
    }
}
