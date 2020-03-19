package temple.generate.target.openapi.ast

import io.circe.Json
import temple.generate.JsonEncodable

private[openapi] trait Response extends JsonEncodable

private[openapi] object Response {

  private[openapi] case class Ref(name: String) extends Response with JsonEncodable.Object {
    override def jsonEntryIterator: Seq[(String, Json)] = Seq("$ref" ~> s"#/components/responses/$name")
  }
}
