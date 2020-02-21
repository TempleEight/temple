package temple.builder

import org.scalatest.{FlatSpec, Matchers}

class DatabaseBuilderTest extends FlatSpec with Matchers {
  behavior of "DatabaseBuilder"

  it should "correctly create a simple users table" in {
    val createQuery = DatabaseBuilder.createServiceTables("temple_user", DatabaseBuilderTestData.sampleService)
    createQuery shouldBe DatabaseBuilderTestData.sampleServiceCreate
  }

  it should "correctly create a complex users table" in {
    val createQuery = DatabaseBuilder.createServiceTables("temple_user", DatabaseBuilderTestData.sampleComplexService)
    createQuery shouldBe DatabaseBuilderTestData.sampleComplexServiceCreate
  }
}
