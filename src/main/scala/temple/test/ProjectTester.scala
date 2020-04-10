package temple.test

import java.io.IOException
import java.net.HttpURLConnection

import scalaj.http.Http
import temple.ast.Metadata.{Provider, ServiceAuth}
import temple.ast.{Metadata, Templefile}
import temple.test.internal.{AuthServiceTest, CRUDServiceTest, ProjectConfig}

import scala.sys.process._

object ProjectTester {

  class EnvironmentVariableNotSetException(name: String)
      extends RuntimeException(s"The environment variable $name was required, but not found")

  /** Execute a command, returning the stdout */
  private def exec(command: String): String =
    s"sh -c '$command'".!!

  /** Configure Kong using the generated script, waiting until this is successful */
  private def configureKong(usesAuth: Boolean, config: ProjectConfig, generatedPath: String): Unit = {
    var configuredKong = false
    while (!configuredKong) {
      exec(
        s"KONG_ADMIN=${config.kongAdminURL} KONG_ENTRY=${config.baseIP} sh $generatedPath/kong/configure-kong.sh",
      )

      try {
        configuredKong =
          if (usesAuth)
            Http("http://localhost:8000/api/auth/login")
              .method("POST")
              .asString
              .code == HttpURLConnection.HTTP_BAD_REQUEST
          // TODO: this might not be true, needs investigating
          else true
        if (!configuredKong) {
          println("Kong wasn't configured correctly - trying again")
          exec("sleep 5")
        }
      } catch {
        case _: IOException => // Keep trying
      }
    }
  }

  /** Configure the infrastructure for testing */
  private def performSetup(templefile: Templefile, generatedPath: String): ProjectConfig = {
    val provider = templefile
      .lookupMetadata[Metadata.Provider]
      .getOrElse { throw new RuntimeException("Could not find deployment information") }

    val config = provider match {
      case Provider.Kubernetes =>
        println("🍪 Spinning up Kubernetes infrastructure...")
        // Require certain env vars to be set
        Seq("REG_URL", "REG_USERNAME", "REG_PASSWORD", "REG_EMAIL").foreach { envVar =>
          if (System.getenv(envVar) == null) throw new EnvironmentVariableNotSetException(envVar)
        }
        exec(s"sh $generatedPath/deploy.sh")

        // Grab Kong's URL for future requests
        val Array(baseURL, kongAdmin, _*) = exec("minikube service kong --url").split("\n")
        val baseIP                        = baseURL.replaceAll("http[s]*://", "")
        ProjectConfig(baseIP, kongAdmin)
      case Provider.DockerCompose =>
        println("🐳 Spinning up Docker Compose infrastructure...")
        exec(
          s"cd $generatedPath && docker ps -a -q | xargs docker rm -f && docker volume prune -f && docker-compose up --build -d",
        )

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
        ProjectConfig("localhost:8000", "localhost:8001")
    }
    configureKong(templefile.usesAuth, config, generatedPath)
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
        exec(s"cd $generatedPath && docker-compose down 2>&1")
    }
  }

  /** Execute the tests on each generated service */
  private def performTests(templefile: Templefile, generatedPath: String, url: String): Unit = {
    val serviceAuths = templefile.services.values.flatMap(_.lookupMetadata[ServiceAuth]).toSet
    var anyFailed    = false
    if (serviceAuths.nonEmpty) {
      anyFailed = AuthServiceTest.test(serviceAuths, url) || anyFailed
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
    * This involves deploying the service locally, performing the tests and then cleaning up
    * @param templefile The parsed & validated templefile
    * @param generatedPath The root of the project where the generated code resides
    */
  def test(templefile: Templefile, generatedPath: String): Unit =
    try {
      val config = performSetup(templefile, generatedPath)
      performTests(templefile, generatedPath, config.baseIP)
    } finally {
      performShutdown(templefile, generatedPath)
    }
}
