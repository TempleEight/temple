package temple.generate.kube.ast.gen

import io.circe.Json
import io.circe.syntax._
import temple.generate.JsonEncodable
import temple.generate.kube.ast.gen.KubeType.{Labels, Metadata}

sealed trait Spec

object Spec {
  case class DeploymentSpec(replicas: Int, selector: Selector, template: Template) extends Spec

  case class Selector(matchLabels: Labels)

  case class Template(metadata: Metadata, spec: PodSpec)

  case class PodSpec(
    hostname: String,
    containers: Seq[Container],
    imagePullSecrets: Seq[Secret],
    restartPolicy: String,
  ) extends Spec

  case class Container(image: String, name: String, ports: Seq[Port])

  case class Port(containerPort: Int)

  case class Secret(name: String) extends JsonEncodable {

    /** Turn a case class into some key-value pairs in preparation for conversion to a JSON object */
    override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq("name" -> name.asJson)
  }
}
