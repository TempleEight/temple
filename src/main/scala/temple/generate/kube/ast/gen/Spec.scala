package temple.generate.kube.ast.gen

import io.circe.Json
import temple.generate.JsonEncodable
import temple.generate.kube.ast.gen.KubeType.{Labels, Metadata}
import temple.generate.kube.ast.gen.volume.AccessMode.AccessMode
import temple.generate.kube.ast.gen.volume.ReclaimPolicy.ReclaimPolicy
import temple.generate.kube.ast.gen.volume.StorageClass.StorageClass

import scala.Option.when

sealed trait Spec extends JsonEncodable.Partial

object Spec {

  case class DeploymentSpec(replicas: Int, selector: Selector, strategy: Option[Strategy], template: Template)
      extends Spec {

    /** Turn a case class into some key-value pairs in preparation for conversion to a JSON object */
    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] =
      Seq(
        "replicas" ~~> Some(replicas),
        "selector" ~~> Some(selector),
        "strategy" ~~> strategy,
        "template" ~~> Some(template),
      )
  }

  case class ServiceSpec(ports: Seq[ServicePort], selector: Labels) extends Spec {

    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
      "ports" ~~> when(ports.nonEmpty)(ports),
      "selector" ~~> Some(selector),
    )
  }

  case class PersistentVolumeSpec(
    storageClass: StorageClass,
    capacityGb: Float,
    accessModes: Seq[AccessMode],
    reclaimPolicy: ReclaimPolicy,
    hostPath: String,
  ) extends Spec {

    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
      "storageClassName" ~~> Some(storageClass),
      "capacity" ~~> Some(Map("storage" -> s"${capacityGb}Gi")),
      "accessModes" ~~> Some(accessModes),
      "persistentVolumeReclaimPolicy" ~~> Some(reclaimPolicy),
      "hostPath" ~~> Some(Map("path" -> hostPath)),
    )
  }

  case class PersistentVolumeClaimSpec(
    accessModes: Seq[AccessMode],
    volumeName: String,
    storageClassName: StorageClass,
    storageResourceRequestAmountMb: Float,
  ) extends Spec {

    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
      "accessModes" ~~> Some(accessModes),
      "volumeName" ~~> Some(volumeName),
      "storageClassName" ~~> Some(storageClassName),
      "resources" ~~> Some(
        Map(
          "requests" -> Map(
            "storage" -> s"${storageResourceRequestAmountMb}Mi",
          ),
        ),
      ),
    )
  }

  case class Selector(matchLabels: Labels) extends JsonEncodable.Object {

    override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq("matchLabels" ~> matchLabels)
  }

  case class Template(metadata: Metadata, spec: PodSpec) extends JsonEncodable.Object {

    override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
      "metadata" ~> metadata,
      "spec" ~> spec,
    )
  }

  case class Strategy(strategy: String) extends JsonEncodable.Object {

    override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq("type" ~> strategy)
  }

  case class PodSpec(
    hostname: String,
    containers: Seq[Container],
    imagePullSecrets: Seq[Secret],
    restartPolicy: String,
    volumes: Seq[Volume],
  ) extends Spec {

    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] =
      Seq(
        "hostname" ~~> Some(hostname),
        "containers" ~~> when(containers.nonEmpty)(containers),
        "imagePullSecrets" ~~> when(imagePullSecrets.nonEmpty)(imagePullSecrets),
        "restartPolicy" ~~> Some(restartPolicy),
        "volumes" ~~> when(volumes.nonEmpty)(volumes),
      )
  }

  case class Volume(name: String, storage: Storage) extends JsonEncodable.Object {

    override def jsonEntryIterator: IterableOnce[(String, Json)] =
      Seq("name" ~> name) ++ storage.jsonEntryIterator
  }

  sealed trait Storage extends JsonEncodable.Object

  case class ConfigMap(name: String) extends Storage {

    override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq("configMap" ~> Map("name" -> name))
  }

  case class PersistentVolume(name: String) extends Storage {

    override def jsonEntryIterator: IterableOnce[(String, Json)] =
      Seq("persistentVolumeClaim" ~> Map("claimName" -> name))
  }

  case class Container(
    image: String,
    name: String,
    ports: Seq[ContainerPort],
    env: Seq[EnvVar],
    volumeMounts: Seq[VolumeMount],
    lifecycle: Option[Lifecycle] = None,
  ) extends JsonEncodable.Partial {

    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] =
      Seq(
        "env" ~~> when(env.nonEmpty)(env),
        "image" ~~> Some(image),
        "name" ~~> Some(name),
        "ports" ~~> when(ports.nonEmpty)(ports),
        "volumeMounts" ~~> when(volumeMounts.nonEmpty)(volumeMounts),
        "lifecycle" ~~> lifecycle,
      )
  }

  case class Lifecycle(command: String) extends JsonEncodable.Object {

    override def jsonEntryIterator: IterableOnce[(String, Json)] =
      Seq(
        "postStart" ~>
        Map(
          "exec" ->
          Map("command" -> Seq("/bin/sh", "-c", command)),
        ),
      )
  }

  case class ContainerPort(containerPort: Int) extends JsonEncodable.Object {

    override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq("containerPort" ~> containerPort)
  }

  case class EnvVar(name: String, value: String) extends JsonEncodable.Object {

    override def jsonEntryIterator: IterableOnce[(String, Json)] =
      Seq("name" ~> name, "value" ~> value)
  }

  case class ServicePort(name: String, port: Int, targetPort: Int) extends JsonEncodable.Object {

    override def jsonEntryIterator: IterableOnce[(String, Json)] =
      Seq(
        "name" ~> name,
        "port" ~> port,
        "targetPort" ~> targetPort,
      )
  }

  case class VolumeMount(mountPath: String, subPath: Option[String], name: String) extends JsonEncodable.Partial {

    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] =
      Seq("mountPath" ~~> Some(mountPath), "subPath" ~~> subPath, "name" ~~> Some(name))
  }

  case class Secret(name: String) extends JsonEncodable.Object {

    override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq("name" ~> name)
  }
}
