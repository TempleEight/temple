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

  it should "generate correct INSERT statements with RETURNING" in {
    PostgresGenerator.generate(TestData.insertStatementWithReturn) shouldBe TestData.postgresInsertStringWithReturn
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

  it should "generate correct update statements with RETURNING" in {
    PostgresGenerator.generate(TestData.updateStatementWithReturn) shouldBe TestData.postgresUpdateStringWithReturn
  }

  it should "generate correct update statements with WHERE and RETURNING" in {
    PostgresGenerator.generate(TestData.updateStatementWithWhereAndReturn) shouldBe TestData.postgresUpdateStringWithWhereAndReturn
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

  it should "handle SELECT statements with prepared WHERE queries correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithWherePrepared) shouldBe TestData.postgresSelectStringWithWherePrepared
  }

  it should "handle SELECT statements with nested prepared WHERE queries correctly" in {
    PostgresGenerator.generate(TestData.readStatementWithNestedWherePrepared) shouldBe TestData.postgresSelectStringWithNestedWherePrepared
  }

  it should "handle SELECT statements with nested prepared WHERE queries correctly with question marks" in {
    val questionMarkContext: PostgresContext = PostgresContext(PreparedType.QuestionMarks)
    PostgresGenerator.generate(TestData.readStatementWithNestedWherePrepared)(questionMarkContext) shouldBe TestData.postgresSelectStringWithNestedWherePreparedUsingQuestionMarks
  }

  it should "handle UPDATE statements with prepared values correctly" in {
    PostgresGenerator.generate(TestData.updateStatementPrepared) shouldBe TestData.postgresUpdateStringPrepared
  }

  it should "handle UPDATE statements with prepared values and WHERE statement correctly" in {
    PostgresGenerator.generate(TestData.updateStatementPreparedWithWhere) shouldBe TestData.postgresUpdateStringPreparedWithWhere
  }

  it should "handle UPDATE statements with prepared values and prepared WHERE statement correctly" in {
    PostgresGenerator.generate(TestData.updateStatementPreparedWithWherePrepared) shouldBe TestData.postgresUpdateStringPreparedWithWherePrepared
  }

  it should "handle UPDATE statements with prepared values and nested prepared WHERE statement correctly" in {
    PostgresGenerator.generate(TestData.updateStatementPreparedWithNestedWherePrepared) shouldBe TestData.postgresUpdateStringPreparedWithNestedWherePrepared
  }

  it should "handle UPDATE statments with prepared and provided values and nested prepared WHERE statement correctly" in {
    PostgresGenerator.generate(TestData.updateStatementPreparedAndValuesWithNestedWherePrepared) shouldBe TestData.postgresUpdateStringPreparedAndValuesWithNestedWherePrepared
  }

  it should "handle DELETE statements with prepared WHERE statement correctly" in {
    PostgresGenerator.generate(TestData.deleteStatementWithWherePrepared) shouldBe TestData.postgresDeleteStringWithWherePrepared
  }

  it should "handle DELETE statements with nested prepared WHERE statement correctly" in {
    PostgresGenerator.generate(TestData.deleteStatementWithNestedWherePrepared) shouldBe TestData.postgresDeleteStringWithNestedWherePrepared
  }
}
