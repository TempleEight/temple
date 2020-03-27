package temple.detail

import temple.ast.AttributeType._
import temple.ast.Metadata.ServiceLanguage
import temple.ast.{Attribute, ProjectBlock, ServiceBlock, Templefile}

import scala.collection.immutable.ListMap

object LanguageSpecificDetailBuilderTestData {

  val sampleService: ServiceBlock = ServiceBlock(
    ListMap(
      "id"          -> Attribute(IntType()),
      "bankBalance" -> Attribute(FloatType()),
      "name"        -> Attribute(StringType()),
      "isStudent"   -> Attribute(BoolType),
      "dateOfBirth" -> Attribute(DateType),
      "timeOfDay"   -> Attribute(TimeType),
      "expiry"      -> Attribute(DateTimeType),
      "image"       -> Attribute(BlobType()),
    ),
  )

  val simpleTemplefile: Templefile = Templefile(
    "test-project",
    ProjectBlock(Seq(ServiceLanguage.Go)),
    services = Map("test-service" -> sampleService),
  )
}
