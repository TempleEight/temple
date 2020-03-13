package temple.generate

import io.circe.{Encoder, Json}

/** Any class that can be encoded to JSON, at least partially using custom functions
  *
  * This trait should be implemented by any root traits, [[temple.generate.JsonEncodable.Object]] and
  * [[temple.generate.JsonEncodable.Partial]] should be implemented by any concrete implementations.
  */
private[generate] trait JsonEncodable {
  protected def toJson: Json

  // Required so that nested JsonEncodable interfaces always call the correct nested version
  implicit final protected def jsonEncoder[T <: JsonEncodable]: Encoder[T] = a => a.toJson
}

private[generate] object JsonEncodable {

  implicit def encodeToJson[T <: JsonEncodable]: Encoder[T] = _.toJson

  /** Implement JsonEncodable by providing a map/key-value-sequence to go in the Json map */
  trait Object extends JsonEncodable {

    /** Turn a case class into some key-value pairs in preparation for conversion to a JSON object */
    def jsonEntryIterator: IterableOnce[(String, Json)]

    // Required so that nested JsonEncodable interfaces always call the correct nested version
    implicit final protected def toJson: Json = Json.obj(jsonEntryIterator.iterator.toSeq: _*)
  }

  /** Like [[temple.generate.JsonEncodable.Object]] but by providing optional values, causing the entries not to render
    * if None is given */
  trait Partial extends Object {
    def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])]

    final override def jsonEntryIterator: IterableOnce[(String, Json)] =
      jsonOptionEntryIterator.iterator.collect { case (str, Some(json)) => (str, json) }
  }
}
