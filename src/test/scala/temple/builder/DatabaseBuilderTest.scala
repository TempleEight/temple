package temple.builder

import org.scalatest.{FlatSpec, Matchers}

class DatabaseBuilderTest extends FlatSpec with Matchers {
  behavior of "DatabaseBuilder"

  it should "correctly create a simple users table" in {
    val createQuery = DatabaseBuilder.createServiceTables("Users", DatabaseBuilderTestData.sampleService)
    createQuery shouldBe DatabaseBuilderTestData.sampleServiceCreate
  }

  it should "correctly create a complex users table" in {
    val createQuery = DatabaseBuilder.createServiceTables("Users", DatabaseBuilderTestData.sampleComplexService)
    createQuery shouldBe DatabaseBuilderTestData.sampleComplexServiceCreate
  }
}
