package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._
import temple.generate.JsonEncodable

private[openapi] trait Response extends JsonEncodable

private[openapi] object Response {

  private[openapi] case class Ref(name: String) extends Response {
    override def jsonEntryIterator: Seq[(String, Json)] = Seq("$ref" -> s"#/components/responses/$name".asJson)
  }
}
