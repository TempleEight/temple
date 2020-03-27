package temple.test

import temple.ast.Templefile

import sys.process._
import scalaj.http.Http

object EndpointTester {

  class EnvironmentVariableNotSetException(name: String)
      extends RuntimeException(s"The environment variable $name was required, but not found")

  private def exec(command: String): String =
    s"sh -c '$command'".!!

  private def performSetup(templefile: Templefile, generatedPath: String): EndpointConfig = {
    println("🍪 Spinning up Kubernetes infrastructure...")
    // Require certain env vars to be set
    Seq("REG_URL", "REG_USERNAME", "REG_PASSWORD", "REG_EMAIL").foreach { envVar =>
      if (Option(System.getenv(envVar)).isEmpty) throw new EnvironmentVariableNotSetException(envVar)
    }

    // Deploy Kubernetes
    exec(s"sh $generatedPath/deploy.sh")

    // Grab Kong's URL for future requests
    val urls   = exec("minikube service kong --url").split("\n")
    val baseIP = urls(0).replaceAll("http[s]*://", "")
    val config = EndpointConfig(baseIP, urls(1))

    // Running deploy.sh doesn't set Kong up correctly for some reason. I couldn't figure it out, so manually call that part
    exec(
      s"KONG_ADMIN=${config.kongAdminURL} KONG_ENTRY=${config.baseIP} sh $generatedPath/kong/configure-kong-k8s.sh",
    )

    config
  }

  def performShutdown(templefile: Templefile, generatedPath: String): Unit = {
    println(s"💀 Shutting down Kubernetes infrastructure...")
    exec("kubectl drain minikube && minikube delete")
  }

  def performTests(templefile: Templefile, generatedPath: String, url: String): Unit = {
    println(s"🧪 Testing user service at $url/api/user...")
    val sampleRequest = Http(s"http://$url/api/user/1").asString.body
    println(sampleRequest)
  }

  def test(templefile: Templefile, generatedPath: String): Unit =
    try {
      val config = performSetup(templefile, generatedPath)
      performTests(templefile, generatedPath, config.baseIP)
    } finally {
      performShutdown(templefile, generatedPath)
    }

}
