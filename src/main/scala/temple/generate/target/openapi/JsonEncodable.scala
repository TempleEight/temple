package temple.generate.target.openapi

import io.circe.{Encoder, Json}

private[openapi] trait JsonEncodable {

  /** Turn a case class into a map in preparation for conversion to a JSON object */
  def jsonEntryIterator: IterableOnce[(String, Json)]

  // Required so that nested JsonEncodable interfaces always call the correct nested version
  implicit final protected def encodeToJson[T <: JsonEncodable]: Encoder[T] = JsonEncodable.encodeToJson
}

private[openapi] object JsonEncodable {

  /** Create an encoder for JSON objects by providing a function to map them to options of values */
  private def mapSequenceEncoder[T](toJsonMap: T => IterableOnce[(String, Json)]): Encoder[T] = (obj: T) => {
    Json.obj(toJsonMap(obj).iterator.toSeq: _*)
  }

  implicit def encodeToJson[T <: JsonEncodable]: Encoder[T] = mapSequenceEncoder(_.jsonEntryIterator)

  trait Partial extends JsonEncodable {
    def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])]

    final override def jsonEntryIterator: IterableOnce[(String, Json)] =
      jsonOptionEntryIterator.iterator.collect { case (str, Some(json)) => (str, json) }
  }
}
