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
  private def mapSequenceEncoder[T](toJsonMap: T => Map[String, Option[Json]]): Encoder[T] =
    (obj: T) => {
      val jsonMap = toJsonMap(obj)

      // collect only the entries that are present
      val somes = jsonMap.iterator.collect { case (str, Some(json)) => (str, json) }.toSeq

      // construct a JSON object from them
      Json.obj(somes: _*)
    }

  implicit def encodeToJson[T <: JsonEncodable]: Encoder[T] = mapSequenceEncoder(_.toJsonMap)
}
