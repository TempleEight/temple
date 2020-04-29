package temple.ast

import temple.collection.enumeration._
import temple.errors.ErrorHandlingContext

/** A piece of metadata modifying a service/project block */
sealed trait Metadata

object Metadata {
  sealed trait ProjectMetadata extends Metadata
  sealed trait ServiceMetadata extends Metadata
  sealed trait StructMetadata  extends ServiceMetadata

  sealed abstract class ServiceLanguage private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with ServiceMetadata
      with ProjectMetadata

  object ServiceLanguage extends Enum[ServiceLanguage] {
    val values: IndexedSeq[ServiceLanguage] = findValues

    case object Go extends ServiceLanguage("Go", "golang")
  }

  sealed abstract class Provider private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with ProjectMetadata

  object Provider extends Enum[Provider] {
    val values: IndexedSeq[Provider] = findValues
    case object Kubernetes    extends Provider("kubernetes", "kube", "k8s", "kubernooties")
    case object DockerCompose extends Provider("dockerCompose", "dc")
  }

  sealed abstract class Database private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with ProjectMetadata
      with ServiceMetadata

  object Database extends Enum[Database] {
    override def values: IndexedSeq[Database] = findValues
    case object Postgres extends Database("postgres", "PostgreSQL")
  }

  sealed abstract class Readable private (name: String)
      extends EnumEntry(name)
      with ServiceMetadata
      with ProjectMetadata

  object Readable extends Enum[Readable] {
    override def values: IndexedSeq[Readable] = findValues
    case object All  extends Readable("all")
    case object This extends Readable("this")
  }

  sealed abstract class Writable private (name: String)
      extends EnumEntry(name)
      with ServiceMetadata
      with ProjectMetadata

  object Writable extends Enum[Writable] {
    override def values: IndexedSeq[Writable] = findValues
    case object All  extends Writable("all")
    case object This extends Writable("this")
  }

  sealed abstract class Endpoint private (name: String) extends EnumEntry(name)

  object Endpoint extends Enum[Endpoint] {
    override def values: IndexedSeq[Endpoint] = findValues
    case object Create extends Endpoint("create")
    case object Read   extends Endpoint("read")
    case object Update extends Endpoint("update")
    case object Delete extends Endpoint("delete")
  }

  case class Omit(endpoints: Set[Endpoint]) extends StructMetadata

  object Omit extends EnumParser[Omit, Seq[String]] {

    def parse(names: Seq[String])(implicit context: ErrorHandlingContext): Omit =
      Omit(names.map(Endpoint.parse(_)).toSet)
  }

  sealed abstract class AuthMethod private (name: String) extends EnumEntry(name) with ProjectMetadata

  object AuthMethod extends Enum[AuthMethod] {
    override def values: IndexedSeq[AuthMethod] = findValues
    case object Email extends AuthMethod("email")
  }

  type ServiceAuth = ServiceAuth.type
  case object ServiceAuth extends ServiceMetadata

  sealed abstract class Metrics private (name: String) extends EnumEntry(name) with ProjectMetadata

  object Metrics extends Enum[Metrics] {
    override def values: IndexedSeq[Metrics] = findValues
    case object Prometheus extends Metrics("prometheus")
  }

  type ServiceEnumerable = ServiceEnumerable.type
  case object ServiceEnumerable          extends StructMetadata
  case class Uses(services: Seq[String]) extends ServiceMetadata
}
