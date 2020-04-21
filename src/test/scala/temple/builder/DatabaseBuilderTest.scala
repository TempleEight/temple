package temple.builder

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.Metadata
import temple.generate.CRUD

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
    val queries = DatabaseBuilder.buildQueries(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.Create),
      isStruct = false,
      Metadata.Readable.All,
    )
    queries.keys should contain(CRUD.Create)
    queries(CRUD.Create) shouldBe DatabaseBuilderTestData.sampleInsertStatement
  }

  it should "correctly build endpoint read queries" in {
    val queries = DatabaseBuilder.buildQueries(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.Read),
      isStruct = false,
      Metadata.Readable.All,
    )
    queries.keys should contain(CRUD.Read)
    queries(CRUD.Read) shouldBe DatabaseBuilderTestData.sampleReadStatement
  }

  it should "correctly build endpoint update queries" in {
    val queries = DatabaseBuilder.buildQueries(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.Update),
      isStruct = false,
      Metadata.Readable.All,
    )
    queries.keys should contain(CRUD.Update)
    queries(CRUD.Update) shouldBe DatabaseBuilderTestData.sampleUpdateStatement
  }

  it should "correctly build endpoint delete queries" in {
    val queries = DatabaseBuilder.buildQueries(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.Delete),
      isStruct = false,
      Metadata.Readable.All,
    )
    queries.keys should contain(CRUD.Delete)
    queries(CRUD.Delete) shouldBe DatabaseBuilderTestData.sampleDeleteStatement
  }

  it should "correctly build endpoint list CreatedByNone queries" in {
    val queries = DatabaseBuilder.buildQueries(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.List),
      isStruct = false,
      Metadata.Readable.All,
    )
    queries.keys should contain(CRUD.List)
    queries(CRUD.List) shouldBe DatabaseBuilderTestData.sampleListStatementEnumerateByAll
  }

  it should "correctly build endpoint list EnumerateByAll queries" in {
    val queries = DatabaseBuilder.buildQueries(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.List),
      isStruct = false,
      Metadata.Readable.All,
    )
    queries.keys should contain(CRUD.List)
    queries(CRUD.List) shouldBe DatabaseBuilderTestData.sampleListStatementEnumerateByAll
  }

  it should "correctly build endpoint list EnumerateByCreator queries" in {
    val queries = DatabaseBuilder.buildQueries(
      "test_service",
      BuilderTestData.sampleService.attributes,
      Set(CRUD.List),
      isStruct = false,
      Metadata.Readable.This,
    )
    queries.keys should contain(CRUD.List)
    queries(CRUD.List) shouldBe DatabaseBuilderTestData.sampleListStatementEnumerateByCreator
  }

  // TODO: test the list endpoint with isStruct=true
}
