package temple.generate.target.openapi.ast

import io.circe.KeyEncoder

sealed abstract class HTTPVerb(val verb: String)

object HTTPVerb {
  case object Get    extends HTTPVerb("get")
  case object Put    extends HTTPVerb("put")
  case object Post   extends HTTPVerb("post")
  case object Patch  extends HTTPVerb("patch")
  case object Delete extends HTTPVerb("delete")

  implicit def httpVerbKeyEncoder: KeyEncoder[HTTPVerb] = _.verb
}
