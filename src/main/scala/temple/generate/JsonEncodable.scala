package temple.generate

import io.circe.export.Exported
import io.circe.generic.encoding.DerivedAsObjectEncoder
import io.circe.generic.util.macros.ExportMacros
import io.circe.syntax._
import io.circe.{Encoder, Json}

import scala.language.existentials
import scala.language.experimental.macros

/** Any class that can be encoded to JSON, at least partially using custom functions
  *
  * This trait should be implemented by any root traits, then the subtraits [[temple.generate.JsonEncodable.Auto]],
  * [[temple.generate.JsonEncodable.Object]] and [[temple.generate.JsonEncodable.Partial]] should be implemented by any
  * concrete implementations.
  */
private[generate] trait JsonEncodable {
  protected def toJson: Json

  // Required so that nested JsonEncodable interfaces always call the correct nested version
  implicit final protected def jsonEncoder[T <: JsonEncodable]: Encoder[T] = a => a.toJson
}

private[generate] object JsonEncodable {

  implicit def encodeToJson[T <: JsonEncodable]: Encoder[T] = _.toJson

  /**
    * Implement JsonEncodable as automatically as possible
    *
    * See the example for the full contents needed to be implemented for a case class called `MyCaseClass`.
    *
    * @example {{{
    *   protected def encoder: ExportedEncoder[this.type] = exportEncoder[MyCaseClass]
    * }}}
    * */
  trait Auto extends JsonEncodable {

    /**
      * The type of any encoder of a supertype of S
      *
      * If you can encode a supertype of S, you can also encode S, with the help of liftEncoder
      */
    protected type ExportedEncoder[-S] = Exported[Encoder.AsObject[C]] forSome { type C >: S }

    /** Use a less specific encoder as a more specific encoder */
    final protected def liftEncoder(encoder: ExportedEncoder[this.type]): Exported[Encoder.AsObject[this.type]] =
      Exported(encoder.instance.contramapObject(x => x))

    /** Copied from [[io.circe.generic.auto.exportEncoder]], as macros can’t be indirected */
    protected final def exportEncoder[A]: Exported[Encoder.AsObject[A]] =
      macro ExportMacros.exportEncoder[DerivedAsObjectEncoder, A]

    /** This needs to be given in order to generate an instance, defined as `exportEncoder[MyClass]`, where MyClass is
      * the class extending JsonEncodable.Auto */
    protected def encoder: ExportedEncoder[this.type]

    final override protected def toJson: Json =
      EncoderOps[this.type](this).asJson(Encoder.importedEncoder(liftEncoder(encoder)))
  }

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
