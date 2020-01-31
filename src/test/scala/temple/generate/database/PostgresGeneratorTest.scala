package temple.generate.database

import org.scalatest.{FlatSpec, Matchers}

class PostgresGeneratorTest extends FlatSpec with Matchers {

  "PostgresGenerator" should "generate correct CREATE statements" in {
    PostgresGenerator.generate(TestData.createStatement) shouldBe TestData.postgresCreateString
  }
  "PostgresGenerator" should "generate correct SELECT statements" in {
    PostgresGenerator.generate(TestData.readStatement) shouldBe TestData.postgresSelectString
  }
  "PostgresGenerator" should "handle column constraints correctly" in {
    PostgresGenerator.generate(TestData.createStatementWithConstraints) shouldBe TestData.postgresCreateStringWithConstraints
  }
  "PostgresGenerator" should "handle selects with WHERE correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithWhere) shouldBe TestData.postgresSelectStringWithWhere
  }
  "PostgresGenerator" should "generate correct INSERT statements" in {
    PostgresGenerator.generate(TestData.insertStatement) shouldBe TestData.postgresInsertString
  }
}
