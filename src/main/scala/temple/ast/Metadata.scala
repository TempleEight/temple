package temple.ast

import temple.collection.enumeration._
import temple.utils.MapUtils.FailThrower

/** A piece of metadata modifying a service/project/target block */
sealed trait Metadata

object Metadata {
  sealed trait TargetMetadata  extends Metadata
  sealed trait ServiceMetadata extends Metadata
  sealed trait ProjectMetadata extends Metadata
  sealed trait StructMetadata  extends Metadata

  sealed abstract class TargetLanguage private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with TargetMetadata

  object TargetLanguage extends Enum[TargetLanguage] {
    val values: IndexedSeq[TargetLanguage] = findValues

    case object Swift      extends TargetLanguage("Swift")
    case object JavaScript extends TargetLanguage("JavaScript", "js")
  }

  sealed abstract class ServiceLanguage private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with ServiceMetadata
      with ProjectMetadata

  object ServiceLanguage extends Enum[ServiceLanguage] {
    val values: IndexedSeq[ServiceLanguage] = findValues

    case object Go extends ServiceLanguage("Go", "golang")
  }

  sealed abstract class Provider private (name: String) extends EnumEntry(name) with ProjectMetadata

  object Provider extends Enum[Provider] {
    val values: IndexedSeq[Provider] = findValues
    case object Aws extends Provider("aws")
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
      with StructMetadata
      with ProjectMetadata

  object Readable extends Enum[Readable] {
    override def values: IndexedSeq[Readable] = findValues
    case object All  extends Readable("all")
    case object This extends Readable("this")
  }

  sealed abstract class Writable private (name: String)
      extends EnumEntry(name)
      with ServiceMetadata
      with StructMetadata
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

  case class Omit(endpoints: Set[Endpoint]) extends ServiceMetadata with StructMetadata

  object Omit {
    def parse(names: Seq[String])(implicit failThrower: FailThrower): Omit = Omit(names.map(Endpoint.parse(_)).toSet)
  }

  case class ServiceAuth(login: String)                 extends ServiceMetadata
  case class ServiceEnumerable(byThis: Boolean = false) extends ServiceMetadata with StructMetadata
  case class TargetAuth(services: Seq[String])          extends TargetMetadata
  case class Uses(services: Seq[String])                extends ServiceMetadata

}
