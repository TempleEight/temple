package generate.database

import org.scalatest.{FlatSpec, Matchers}

class PostgresGeneratorTest extends FlatSpec with Matchers {

  "PostgresGenerator" should "generate correct CREATE statements" in {
    PostgresGenerator.generate(TestData.createStatement) shouldBe TestData.createString
  }

  "PostgresGenerator" should "handle column constrants correctly" in {
    PostgresGenerator.generate(TestData.createStatement) shouldBe TestData.createString
  }
}
