package temple.ast

import temple.DSL.semantics.SemanticContext
import temple.collection.enumeration._

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

  object TargetLanguage extends Enum[TargetLanguage, SemanticContext] {
    val values: IndexedSeq[TargetLanguage] = findValues

    case object Swift      extends TargetLanguage("Swift")
    case object JavaScript extends TargetLanguage("JavaScript", "js")
  }

  sealed abstract class ServiceLanguage private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with ServiceMetadata
      with ProjectMetadata

  object ServiceLanguage extends Enum[ServiceLanguage, SemanticContext] {
    val values: IndexedSeq[ServiceLanguage] = findValues

    case object Go extends ServiceLanguage("Go", "golang")
  }

  sealed abstract class Provider private (name: String) extends EnumEntry(name) with ProjectMetadata

  object Provider extends Enum[Provider, SemanticContext] {
    val values: IndexedSeq[Provider] = findValues
    case object Aws extends Provider("aws")
  }

  sealed abstract class Database private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with ProjectMetadata
      with ServiceMetadata

  object Database extends Enum[Database, SemanticContext] {
    override def values: IndexedSeq[Database] = findValues
    case object Postgres extends Database("postgres", "PostgreSQL")
  }

  sealed abstract class Readable private (name: String)
      extends EnumEntry(name)
      with ServiceMetadata
      with StructMetadata
      with ProjectMetadata

  object Readable extends Enum[Readable, SemanticContext] {
    override def values: IndexedSeq[Readable] = findValues
    case object All  extends Readable("all")
    case object This extends Readable("this")
  }

  sealed abstract class Writable private (name: String)
      extends EnumEntry(name)
      with ServiceMetadata
      with StructMetadata
      with ProjectMetadata

  object Writable extends Enum[Writable, SemanticContext] {
    override def values: IndexedSeq[Writable] = findValues
    case object All  extends Writable("all")
    case object This extends Writable("this")
  }

  sealed abstract class Endpoint private (name: String) extends EnumEntry(name)

  object Endpoint extends Enum[Endpoint, SemanticContext] {
    override def values: IndexedSeq[Endpoint] = findValues
    case object Create extends Endpoint("create")
    case object Read   extends Endpoint("read")
    case object Update extends Endpoint("update")
    case object Delete extends Endpoint("delete")
  }

  case class Omit(endpoints: Seq[Endpoint]) extends ServiceMetadata with StructMetadata

  object Omit extends EnumParser[Omit, Seq[String], SemanticContext] {

    def parse(names: Seq[String])(implicit context: SemanticContext): Omit =
      Omit(names.map(Endpoint.parse(_)))
  }

  case class ServiceAuth(login: String)                 extends ServiceMetadata
  case class ServiceEnumerable(byThis: Boolean = false) extends ServiceMetadata with StructMetadata
  case class TargetAuth(services: Seq[String])          extends TargetMetadata
  case class Uses(services: Seq[String])                extends ServiceMetadata

}
