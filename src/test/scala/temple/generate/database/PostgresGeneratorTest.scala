package temple.generate.database

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.database.{PostgresGeneratorTestData => TestData}

class PostgresGeneratorTest extends FlatSpec with Matchers {

  implicit val context: PostgresContext = PostgresContext(PreparedType.DollarNumbers)

  behavior of "PostgresGenerator"

  it should "generate correct CREATE statements" in {
    PostgresGenerator.generate(TestData.createStatement) shouldBe TestData.postgresCreateString
  }

  it should "generate correct SELECT statements" in {
    PostgresGenerator.generate(TestData.readStatement) shouldBe TestData.postgresSelectString
  }

  it should "handle column constraints correctly" in {
    PostgresGenerator.generate(TestData.createStatementWithConstraints) shouldBe TestData.postgresCreateStringWithConstraints
  }

  it should "handle selects with WHERE correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithWhere) shouldBe TestData.postgresSelectStringWithWhere
  }

  it should "generate correct INSERT statements" in {
    PostgresGenerator.generate(TestData.insertStatement) shouldBe TestData.postgresInsertString
  }

  it should "generate correct INSERT statements with question marks" in {
    val questionMarkContext: PostgresContext = PostgresContext(PreparedType.QuestionMarks)
    PostgresGenerator.generate(TestData.insertStatement)(questionMarkContext) shouldBe TestData.postgresInsertStringWithQuestionMarks
  }

  it should "handle conjunctions in WHERE correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithWhereConjunction) shouldBe TestData.postgresSelectStringWithWhereConjunction
  }

  it should "handle disjunctions in WHERE correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithWhereDisjunction) shouldBe TestData.postgresSelectStringWithWhereDisjunction
  }

  it should "handle inverse in WHERE correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithWhereInverse) shouldBe TestData.postgresSelectStringWithWhereInverse
  }

  it should "generate correct UPDATE statements" in {
    PostgresGenerator.generate(TestData.updateStatement) shouldBe TestData.postgresUpdateString
  }

  it should "handle updates with WHERE correctly" in {
    PostgresGenerator.generate(TestData.updateStatementWithWhere) shouldBe TestData.postgresUpdateStringWithWhere
  }

  it should "generate correct DELETE statements" in {
    PostgresGenerator.generate(TestData.deleteStatement) shouldBe TestData.postgresDeleteString
  }

  it should "handle deletes with WHERE correctly" in {
    PostgresGenerator.generate(TestData.deleteStatementWithWhere) shouldBe TestData.postgresDeleteStringWithWhere
  }

  it should "handle DROP TABLEs correctly" in {
    PostgresGenerator.generate(TestData.dropStatement) shouldBe TestData.postgresDropString
  }

  it should "handle DROP TABLE IF EXISTS correctly" in {
    PostgresGenerator.generate(TestData.dropStatementIfExists) shouldBe TestData.postgresDropStringIfExists
  }

  it should "handle complex SELECT statements" in {
    PostgresGenerator.generate(TestData.readStatementComplex) shouldBe TestData.postgresSelectStringComplex
  }
}
