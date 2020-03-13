package temple.generate.target.openapi

import io.circe.Json
import temple.generate.JsonEncodable

private[openapi] trait RequestBody extends JsonEncodable

private[openapi] object RequestBody {

  private[openapi] case class Ref(name: String) extends RequestBody with JsonEncodable.Object {
    override def jsonEntryIterator: Seq[(String, Json)] = Seq("$ref" ~> s"#/components/requestBody/$name")
  }
}
