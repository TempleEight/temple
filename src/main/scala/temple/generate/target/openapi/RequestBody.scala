package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._
import temple.generate.JsonEncodable

private[openapi] trait RequestBody extends JsonEncodable

private[openapi] object RequestBody {

  private[openapi] case class Ref(name: String) extends RequestBody {
    override def jsonEntryIterator: Seq[(String, Json)] = Seq("$ref" -> s"#/components/requestBody/$name".asJson)
  }
}
