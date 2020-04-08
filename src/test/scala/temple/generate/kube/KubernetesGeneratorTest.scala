package temple.generate.kube

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.FileSystem.File

class KubernetesGeneratorTest extends FlatSpec with Matchers {

  behavior of "KubernetesGenerator"

  it should "generate correct Deployment headers" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "deployment.yaml")
    output.keys should contain(file)
    output(file) should startWith(UnitTestData.userDeploymentHeader)
  }

  it should "generate correct Service headers" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "service.yaml")
    output.keys should contain(file)
    output(file) should startWith(UnitTestData.userServiceHeader)
  }

  it should "generate correct Db-Deployment headers" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "db-deployment.yaml")
    output.keys should contain(file)
    output(file) should startWith(UnitTestData.userDbDeploymentHeader)
  }

  it should "generate correct Db-Service headers" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "db-service.yaml")
    output.keys should contain(file)
    output(file) should startWith(UnitTestData.userDbServiceHeader)
  }

  it should "generate correct Db-Storage headers" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "db-storage.yaml")
    output.keys should contain(file)
    output(file) should startWith(UnitTestData.userDbStorageVolumeHeader)
    output(file) should include(UnitTestData.userDbStorageClaimHeader)
  }

  it should "generate correct deployments" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "deployment.yaml")
    output.keys should contain(file)
    output(file) should be(UnitTestData.userDeployment)
  }

  it should "generate correct services" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "service.yaml")
    output.keys should contain(file)
    output(file) should be(UnitTestData.userService)
  }

  it should "generate correct database deployments" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "db-deployment.yaml")
    output.keys should contain(file)
    output(file) should be(UnitTestData.userDbDeployment)
  }

  it should "generate correct database services" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "db-service.yaml")
    output.keys should contain(file)
    output(file) should be(UnitTestData.userDbService)
  }

  it should "generate correct database storage" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    val file   = File("kube/user", "db-storage.yaml")
    output.keys should contain(file)
    output(file) should be(UnitTestData.userDbStorage)
  }

  it should "generate all the required kong files" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    output.keys should contain(File("kube/kong", "kong-deployment.yaml"))
    output.keys should contain(File("kube/kong", "kong-db-deployment.yaml"))
    output.keys should contain(File("kube/kong", "kong-db-service.yaml"))
    output.keys should contain(File("kube/kong", "kong-service.yaml"))
    output.keys should contain(File("kube/kong", "kong-migration-job.yaml"))
  }

  it should "generate a kong config" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    output.keys should contain(File("kong", "configure-kong.sh"))
  }

  it should "generate a correct deploy script with metrics" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    output.keys should contain(File("", "deploy.sh"))
    output(File("", "deploy.sh")) should be(UnitTestData.userDeployScriptWithMetrics)
  }

  it should "generate a correct deploy script without metrics" in {
    val output = KubernetesGenerator.generate("example", UnitTestData.basicOrchestrationRootWithoutMetrics)
    output.keys should contain(File("", "deploy.sh"))
    output(File("", "deploy.sh")) should be(UnitTestData.userDeployScriptWithoutMetrics)
  }
}
