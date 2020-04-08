package temple.test

import scalaj.http.Http
import temple.ast.Metadata.{Provider, ServiceAuth}
import temple.ast.{Metadata, Templefile}
import temple.test.internal.{AuthServiceTest, CRUDServiceTest, ProjectConfig}

import scala.sys.process._
import scala.util.Try

object ProjectTester {

  class EnvironmentVariableNotSetException(name: String)
      extends RuntimeException(s"The environment variable $name was required, but not found")

  /** Execute a command, returning the stdout */
  private def exec(command: String): String =
    s"sh -c '$command'".!!

  /** Configure the infrastructure for testing */
  private def performSetup(templefile: Templefile, generatedPath: String): ProjectConfig = {
    val provider = templefile
      .lookupMetadata[Metadata.Provider]
      .getOrElse(throw new RuntimeException("Could not find deployment information"))

    val config = provider match {
      case Provider.Kubernetes =>
        println("🍪 Spinning up Kubernetes infrastructure...")
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

        var finishedStarting = false
        while (!finishedStarting) {
          val kongRequest = Try(Http("http://localhost:8001/status").asString)
          kongRequest.map { request =>
            finishedStarting = request.code == 200
          }
          exec("sleep 5")
        }
        ProjectConfig("localhost:8000", "localhost:8001")
    }

    exec(
      s"KONG_ADMIN=${config.kongAdminURL} KONG_ENTRY=${config.baseIP} sh $generatedPath/kong/configure-kong.sh",
    )
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
        exec(s"cd $generatedPath && docker-compose down")
    }
  }

  /** Execute the tests on each generated service */
  private def performTests(templefile: Templefile, generatedPath: String, url: String): Unit = {
    val serviceAuths = templefile.services.values.flatMap(_.lookupMetadata[ServiceAuth]).toSet
    if (serviceAuths.nonEmpty) {
      AuthServiceTest.test(serviceAuths, url)
    }
    templefile.providedServices.foreach {
      case (name, block) =>
        CRUDServiceTest.test(name, block, templefile.providedServices, url)
    }
  }

  /**
    * Perform a full endpoint test on a generated project, by querying each endpoint and validating responses
    * This involves deploying the service locally, performing the tests and then cleaning up
    * @param templefile The parsed & validated templefile
    * @param generatedPath The root of the project where the generated code resides
    */
  def test(templefile: Templefile, generatedPath: String): Unit = {
    // Require certain env vars to be set
    Seq("REG_URL", "REG_USERNAME", "REG_PASSWORD", "REG_EMAIL").foreach { envVar =>
      if (System.getenv(envVar) == null) throw new EnvironmentVariableNotSetException(envVar)
    }

    try {
      val config = performSetup(templefile, generatedPath)
      performTests(templefile, generatedPath, config.baseIP)
    } finally {
      performShutdown(templefile, generatedPath)
    }
  }

}
