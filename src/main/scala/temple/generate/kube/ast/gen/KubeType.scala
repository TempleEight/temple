package temple.generate.kube.ast.gen

import io.circe.Json
import io.circe.syntax._
import temple.generate.JsonEncodable
import temple.generate.kube.GenType

private[kube] object KubeType {

  /** Header represents the header of a Kubernetes yaml file */
  case class Header(apiVersion: String, kind: String, metadata: Metadata)

  case class Body(spec: DeploymentSpec)

  /** A Kubernetes yaml file metadata block */
  case class Metadata(name: String, labels: Labels)

  /** A Kubernetes yaml file labels block */
  case class Labels(name: String, genType: GenType, isDb: Boolean) extends JsonEncodable.Partial {

    /** Turn a case class into some key-value pairs in preparation for conversion to a JSON object
      * The `type` label should only be set on PersistentVolume objects */
    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] =
      Seq(
        "app"  -> Some(name.asJson),
        "type" -> Option.when(genType == GenType.StorageMount)("local".asJson),
        "kind" -> Option.when(genType == GenType.Deployment)(if (isDb) "db".asJson else "service".asJson),
      )
  }

  case class DeploymentSpec(replicas: Int, selector: Selector, template: Template)

  case class Selector(matchLabels: Labels)

  case class Template(metadata: Metadata, spec: PodSpec)

  case class PodSpec(
    hostname: String,
    containers: Seq[Container],
    imagePullSecrets: Seq[Secret],
    restartPolicy: String,
  )

  case class Container(image: String, name: String, ports: Seq[Port])

  case class Port(containerPort: Int)

  case class Secret(name: String) extends JsonEncodable {

    /** Turn a case class into some key-value pairs in preparation for conversion to a JSON object */
    override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq("name" -> name.asJson)
  }

}
