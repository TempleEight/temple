package temple.generate.orchestration.dockercompose.ast

import io.circe.Json
import temple.generate.JsonEncodable

import scala.collection.immutable.ListMap

abstract private[dockercompose] class Service(ports: Seq[Int], networks: Seq[String]) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "ports" ~> ports.map { port =>
      s"$port:$port"
    },
    "networks" ~> networks,
  )
}

object Service {

  // Generate a service in Docker Compose which is to be built from a local path
  private[dockercompose] case class LocalService(path: String, ports: Seq[Int], networks: Seq[String])
      extends Service(ports, networks) {

    override def jsonEntryIterator: IterableOnce[(String, Json)] =
      ListMap(
        "build" ~> path,
      ) ++ super.jsonEntryIterator
  }

  // Generate a service in Docker Compose which is pulled from an existing image
  private[dockercompose] case class ExternalService(
    image: String,
    environment: Seq[(String, String)],
    volumes: Seq[(String, String)],
    ports: Seq[Int],
    networks: Seq[String],
  ) extends Service(ports, networks) {

    override def jsonEntryIterator: IterableOnce[(String, Json)] =
      ListMap(
        "image"       ~> image,
        "environment" ~> environment.map { case (k, v) => s"$k=$v" },
        "volumes"     ~> volumes.map { case (k, v) => s"$k:$v" },
      ) ++ super.jsonEntryIterator

  }

}
