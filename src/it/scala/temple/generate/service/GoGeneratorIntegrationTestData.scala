package temple.generate.service

import temple.generate.Crud

object GoGeneratorIntegrationTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    "user",
    "github.com/TempleEight/spec-golang/user",
    Seq.empty,
    Set(Crud.Create, Crud.Read, Crud.Update, Crud.Delete),
    80,
  )

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      "match",
      "github.com/TempleEight/spec-golang/match",
      Seq("user"),
      Set(Crud.ReadAll, Crud.Create, Crud.Read, Crud.Update, Crud.Delete),
      81,
    )
}
