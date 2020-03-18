package temple.generate.server

import temple.generate.CRUD

object GoGeneratorIntegrationTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    "user",
    "github.com/TempleEight/spec-golang/user",
    Seq.empty,
    Set(CRUD.Create, CRUD.Read, CRUD.Update, CRUD.Delete),
    80,
  )

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      "match",
      "github.com/TempleEight/spec-golang/match",
      Seq("user"),
      Set(CRUD.ReadAll, CRUD.Create, CRUD.Read, CRUD.Update, CRUD.Delete),
      81,
    )
}
