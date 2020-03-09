package temple.generate.target.openapi

import io.circe.{Encoder, Json}

private[openapi] trait JsonEncodable {

  /** Turn a case class into a map in preparation for conversion to a JSON object */
  def toJsonMap: Map[String, Option[Json]]

  // Required so that nested JsonEncodable interfaces always call the correct nested version
  implicit final protected def encodeToJson[T <: JsonEncodable]: Encoder[T] = JsonEncodable.encodeToJson
}

private[openapi] object JsonEncodable {

  /** Create an encoder for JSON objects by providing a function to map them to options of values */
  private def mapSequenceEncoder[T](f: T => Map[String, Option[Json]]): Encoder[T] =
    (a: T) => Json.obj(f(a).iterator.flatMap { case (str, maybeJson) => maybeJson.map(str -> _) }.toSeq: _*)

  implicit def encodeToJson[T <: JsonEncodable]: Encoder[T] = mapSequenceEncoder(_.toJsonMap)
}
