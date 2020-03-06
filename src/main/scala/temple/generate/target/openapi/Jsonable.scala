package temple.generate.target.openapi

import io.circe.{Encoder, Json}

private[openapi] trait Jsonable {

  /** Turn a case class into a map in preparation for conversion to a JSON object */
  def toJsonMap: Map[String, Option[Json]]

  // Required so that nested Jsonable interfaces always call the correct nested version
  implicit final protected def encodeJsonable[T <: Jsonable]: Encoder[T] = Jsonable.encodeJsonable
}

private[openapi] object Jsonable {

  /** Create an encoder for JSON objects by providing a function to map them to options of values */
  private def mapSequenceEncoder[T](f: T => Map[String, Option[Json]]): Encoder[T] =
    (a: T) => Json.obj(f(a).iterator.flatMap { case (str, maybeJson) => maybeJson.map(str -> _) }.toSeq: _*)

  implicit def encodeJsonable[T <: Jsonable]: Encoder[T] = mapSequenceEncoder(_.toJsonMap)
}
