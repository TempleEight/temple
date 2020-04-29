package temple.generate.orchestration.kube.ast

import io.circe.Json
import temple.generate.JsonEncodable
import temple.generate.orchestration.kube.GenType

import scala.Option.when

private[kube] object KubeType {

  /** Header represents the header of a Kubernetes yaml file */
  case class Header(apiVersion: String, kind: String, metadata: Metadata)

  case class Body[T <: Spec](spec: T)

  /** A Kubernetes yaml file metadata block */
  case class Metadata(name: String, labels: Labels) extends JsonEncodable.Object {
    override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq("name" ~> name, "labels" ~> labels)
  }

  /** A Kubernetes yaml file labels block */
  case class Labels(name: String, genType: GenType, isDb: Boolean) extends JsonEncodable.Partial {

    /** Turn a case class into some key-value pairs in preparation for conversion to a JSON object
      * The `type` label should only be set on PersistentVolume objects */
    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] =
      Seq(
        "app"  ~~> Some(name),
        "type" ~~> when(genType == GenType.StorageMount)("local"),
        "kind" ~~> when(genType == GenType.Deployment || genType == GenType.Service) { if (isDb) "db" else "service" },
      )
  }
}
