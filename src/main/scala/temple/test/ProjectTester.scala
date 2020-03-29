package temple.test

import temple.ast.Metadata.ServiceAuth
import temple.ast.Templefile
import temple.test.internal.{AuthServiceTest, ProjectConfig}

import scala.sys.process._

object ProjectTester {

  class EnvironmentVariableNotSetException(name: String)
      extends RuntimeException(s"The environment variable $name was required, but not found")

  /** Execute a command, returning the stdout */
  private def exec(command: String): String =
    s"sh -c '$command'".!!

  /** Configure the infrastructure for testing */
  private def performSetup(templefile: Templefile, generatedPath: String): ProjectConfig = {
    println("🍪 Spinning up Kubernetes infrastructure...")
    // Deploy Kubernetes
    exec(s"sh $generatedPath/deploy.sh")

    // Grab Kong's URL for future requests
    val minikubeURLString = exec("minikube service kong --url")
    val urls              = minikubeURLString.split("\n")
    val baseIP            = urls(0).replaceAll("http[s]*://", "")
    val config            = ProjectConfig(baseIP, urls(1))

    // Running deploy.sh doesn't set Kong up correctly for some reason. I couldn't figure it out, so manually call that part
    exec(
      s"KONG_ADMIN=${config.kongAdminURL} KONG_ENTRY=${config.baseIP} sh $generatedPath/kong/configure-kong-k8s.sh",
    )

    config
  }

  /** Gracefully shutdown the infrastructure */
  private def performShutdown(templefile: Templefile, generatedPath: String): Unit = {
    println(s"💀 Shutting down Kubernetes infrastructure...")
    exec("kubectl drain minikube && minikube delete")
  }

  /** Execute the tests on each generated service */
  private def performTests(templefile: Templefile, generatedPath: String, url: String): Unit = {
    val serviceAuths = templefile.services.values.flatMap(_.lookupMetadata[ServiceAuth]).toSet
    if (serviceAuths.nonEmpty) {
      AuthServiceTest.test(serviceAuths, url)
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
