package temple.generate.kube.ast.gen

import io.circe.Json
import io.circe.syntax._
import temple.generate.JsonEncodable
import temple.generate.kube.GenType

/** Header represents the header of a Kubernetes yaml file */
case class Header(apiVersion: String, kind: String, metadata: Metadata)

/** A Kubernetes yaml file metadata block */
case class Metadata(name: String, labels: Labels)

/** A kubernetes yaml file labels block */
case class Labels(name: String, genType: GenType) extends JsonEncodable.Partial {

  /** Turn a case class into some key-value pairs in preparation for conversion to a JSON object
   * The `type` label should only be set on PersistentVolume objects*/
  override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] =
    Seq(
    ("app" -> Some(name.asJson))
    ,"type" -> Option.when(genType == GenType.StorageMount)("local".asJson)
  )
}