package temple.generate.language.service

import temple.generate.language.service.adt._
import temple.utils.FileUtils._

object GoGeneratorIntegrationTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    "user",
    "github.com/TempleEight/spec-golang/user",
    Seq.empty,
    Set(Endpoint.Create, Endpoint.Read, Endpoint.Update, Endpoint.Delete),
    80,
  )

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      "match",
      "github.com/TempleEight/spec-golang/match",
      Seq("user"),
      Set(Endpoint.ReadAll, Endpoint.Create, Endpoint.Read, Endpoint.Update, Endpoint.Delete),
      81,
    )
}
