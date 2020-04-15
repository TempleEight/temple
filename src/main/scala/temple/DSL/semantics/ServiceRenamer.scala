package temple.DSL.semantics

import temple.ast.AbstractAttribute.Attribute
import temple.ast.AbstractServiceBlock._
import temple.ast._

case class ServiceRenamer(renamingMap: Map[String, String]) {

  private def rename(string: String): String =
    renamingMap.getOrElse(string, { throw new NoSuchElementException(s"Key $string missing from renaming map") })

  private def renameTargetBlock(block: TargetBlock): TargetBlock =
    // currently `identity`, as language is the only metadata
    TargetBlock(block.metadata.map {
      case language: Metadata.TargetLanguage => language
    })

  private def renameProjectBlock(block: ProjectBlock): ProjectBlock =
    // currently `identity`, as no metadata contains a service name
    ProjectBlock(
      block.metadata.map {
        case authMethod: Metadata.AuthMethod    => authMethod
        case language: Metadata.ServiceLanguage => language
        case provider: Metadata.Provider        => provider
        case database: Metadata.Database        => database
        case readable: Metadata.Readable        => readable
        case writable: Metadata.Writable        => writable
        case metrics: Metadata.Metrics          => metrics
      },
    )

  private def renameTargetBlocks(targets: Map[String, TargetBlock]): Map[String, TargetBlock] =
    targets.view.mapValues(renameTargetBlock).toMap

  private def renameServiceMetadata(metadata: Metadata.ServiceMetadata): Metadata.ServiceMetadata = metadata match {
    // rename any services referenced in #uses
    case Metadata.Uses(services) => Metadata.Uses(services.map(rename))
    // otherwise just return the other metadata, as it contains no service names
    case language: Metadata.ServiceLanguage => language
    case database: Metadata.Database        => database
    case readable: Metadata.Readable        => readable
    case writable: Metadata.Writable        => writable
    case omit: Metadata.Omit                => omit
    case auth: Metadata.ServiceAuth         => auth
    case Metadata.ServiceEnumerable         => Metadata.ServiceEnumerable
  }

  def renameStructMetadata(metadata: Metadata.StructMetadata): Metadata.StructMetadata = metadata match {
    // currently `identity`, as no metadata contains a service name
    case readable: Metadata.Readable => readable
    case writable: Metadata.Writable => writable
    case omit: Metadata.Omit         => omit
    case Metadata.ServiceEnumerable  => Metadata.ServiceEnumerable
  }

  private def renameStructBlock(block: StructBlock): StructBlock =
    StructBlock(renameAttributes(block.attributes), block.metadata.map(renameStructMetadata))

  private def renameStructBlocks(structs: Map[String, StructBlock]): Map[String, StructBlock] =
    structs.map { case (name, block) => rename(name) -> renameStructBlock(block) }

  private def renameAttributeType(attributeType: AttributeType): AttributeType = attributeType match {
    case AttributeType.ForeignKey(references)                => AttributeType.ForeignKey(rename(references))
    case attributeType: AttributeType.PrimitiveAttributeType => attributeType
  }

  private def renameAttribute(attribute: AbstractAttribute): AbstractAttribute = attribute match {
    case _: Attribute =>
      Attribute(
        renameAttributeType(attribute.attributeType),
        attribute.accessAnnotation,
        attribute.valueAnnotations,
      )
    case default => default
  }

  private def renameAttributes(attributes: Map[String, AbstractAttribute]): Map[String, AbstractAttribute] =
    attributes.view.mapValues(renameAttribute).to(attributes.mapFactory)

  private def renameServiceBlock(block: ServiceBlock): ServiceBlock =
    ServiceBlock(
      renameAttributes(block.attributes),
      block.metadata.map(renameServiceMetadata),
      renameStructBlocks(block.structs),
    )

  private def renameServiceBlocks(services: Map[String, ServiceBlock]): Map[String, ServiceBlock] = services.map {
    case (name, block) => rename(name) -> renameServiceBlock(block)
  }

  def apply(templefile: Templefile): Templefile =
    Templefile(
      templefile.projectName,
      renameProjectBlock(templefile.projectBlock),
      renameTargetBlocks(templefile.targets),
      renameServiceBlocks(templefile.services),
    )
}
