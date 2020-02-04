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
  "PostgresGenerator" should "handle conjunctions in WHERE correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithWhereConjunction) shouldBe TestData.postgresSelectStringWithWhereConjunction
  }
  "PostgresGenerator" should "handle disjunctions in WHERE correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithWhereDisjunction) shouldBe TestData.postgresSelectStringWithWhereDisjunction
  }
  "PostgresGenerator" should "handle inverse in WHERE correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithWhereInverse) shouldBe TestData.postgresSelectStringWithWhereInverse
  }
  "PostgresGenerator" should "generate correct DELETE statements" in {
    PostgresGenerator.generate(TestData.deleteStatement) shouldBe TestData.postgresDeleteString
  }
  "PostgresGenerator" should "handle deletes with WHERE correctly" in {
    PostgresGenerator.generate(TestData.deleteStatementWithWhere) shouldBe TestData.postgresDeleteStringWithWhere
  }
  "PostgresGenerator" should "handle DROP TABLEs correctly" in {
    PostgresGenerator.generate(TestData.dropStatement) shouldBe TestData.postgresDropString
  }
  "PostgresGenerator" should "handle DROP TABLE IF EXISTS correctly" in {
    PostgresGenerator.generate(TestData.dropStatementIfExists) shouldBe TestData.postgresDropStringIfExists
  }
  "PostgresGenerator" should "handle complex SELECT statements" in {
    PostgresGenerator.generate(TestData.readStatementComplex) shouldBe TestData.postgresSelectStringComplex
  }
}
