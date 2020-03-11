package temple.generate.target.openapi

import io.circe.KeyEncoder

object HTTPVerb extends Enumeration {
  type HTTPVerb = Value
  val Get: HTTPVerb    = Value("get")
  val Put: HTTPVerb    = Value("put")
  val Post: HTTPVerb   = Value("post")
  val Patch: HTTPVerb  = Value("patch")
  val Delete: HTTPVerb = Value("delete")

  implicit def httpVerbKeyEncoder: KeyEncoder[HTTPVerb] = _.toString
}
