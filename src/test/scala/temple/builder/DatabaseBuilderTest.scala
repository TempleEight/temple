package temple.builder

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.CRUD
import temple.generate.server.CreatedByAttribute

class DatabaseBuilderTest extends FlatSpec with Matchers {

  behavior of "DatabaseBuilder"

  it should "correctly create a simple users table" in {
    val createQuery = DatabaseBuilder.createServiceTables("temple_user", BuilderTestData.sampleService)
    createQuery shouldBe DatabaseBuilderTestData.sampleServiceCreate
  }

  it should "correctly create a complex users table" in {
    val createQuery = DatabaseBuilder.createServiceTables("temple_user", BuilderTestData.sampleComplexService)
    createQuery shouldBe DatabaseBuilderTestData.sampleComplexServiceCreate
  }

  it should "correctly build endpoint create queries" in {
    val queries = DatabaseBuilder.buildQuery(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.Create),
      CreatedByAttribute.None,
    )
    queries.keys should contain(CRUD.Create)
    queries(CRUD.Create) shouldBe DatabaseBuilderTestData.sampleInsertStatement
  }

  it should "correctly build endpoint read queries" in {
    val queries = DatabaseBuilder.buildQuery(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.Read),
      CreatedByAttribute.None,
    )
    queries.keys should contain(CRUD.Read)
    queries(CRUD.Read) shouldBe DatabaseBuilderTestData.sampleReadStatement
  }

  it should "correctly build endpoint update queries" in {
    val queries = DatabaseBuilder.buildQuery(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.Update),
      CreatedByAttribute.None,
    )
    queries.keys should contain(CRUD.Update)
    queries(CRUD.Update) shouldBe DatabaseBuilderTestData.sampleUpdateStatement
  }

  it should "correctly build endpoint delete queries" in {
    val queries = DatabaseBuilder.buildQuery(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.Delete),
      CreatedByAttribute.None,
    )
    queries.keys should contain(CRUD.Delete)
    queries(CRUD.Delete) shouldBe DatabaseBuilderTestData.sampleDeleteStatement
  }

  it should "correctly build endpoint list CreatedByNone queries" in {
    val queries = DatabaseBuilder.buildQuery(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.List),
      CreatedByAttribute.None,
    )
    queries.keys should contain(CRUD.List)
    queries(CRUD.List) shouldBe DatabaseBuilderTestData.sampleListStatementEnumerateByAll
  }

  it should "correctly build endpoint list EnumerateByAll queries" in {
    val queries = DatabaseBuilder.buildQuery(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.List),
      CreatedByAttribute.EnumerateByAll("created_by", "created_by"),
    )
    queries.keys should contain(CRUD.List)
    queries(CRUD.List) shouldBe DatabaseBuilderTestData.sampleListStatementEnumerateByAll
  }

  it should "correctly build endpoint list EnumerateByCreator queries" in {
    val queries = DatabaseBuilder.buildQuery(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.List),
      CreatedByAttribute.EnumerateByCreator("created_by", "created_by"),
    )
    queries.keys should contain(CRUD.List)
    queries(CRUD.List) shouldBe DatabaseBuilderTestData.sampleListStatementEnumerateByCreator
  }
}
