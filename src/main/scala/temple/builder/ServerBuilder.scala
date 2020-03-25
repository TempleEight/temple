package temple.builder

import temple.ast.AttributeType.{ForeignKey, PrimitiveAttributeType}
import temple.ast.Metadata.{ServiceEnumerable, ServiceLanguage}
import temple.ast.{AttributeType, ServiceBlock}
import temple.builder.project.LanguageConfig.GoLanguageConfig
import temple.builder.project.ProjectConfig
import temple.generate.CRUD
import temple.generate.server.{CreatedByAttribute, IDAttribute, ServiceRoot}

import scala.collection.immutable.ListMap

object ServerBuilder {

  def buildServiceRoot(
    serviceName: String,
    serviceBlock: ServiceBlock,
    port: Int,
    endpoints: Set[CRUD],
  ): ServiceRoot = {
    val language = serviceBlock.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    val languageConfig = language match {
      case ServiceLanguage.Go => GoLanguageConfig
    }

    val createdBy: CreatedByAttribute = serviceBlock.lookupMetadata[ServiceEnumerable] match {
      case Some(ServiceEnumerable(true)) =>
        CreatedByAttribute.EnumerateByCreator(
          languageConfig.createdByInputName,
          languageConfig.createdByName,
          languageConfig.createdByAttributeType,
        )
      case Some(ServiceEnumerable(false)) =>
        CreatedByAttribute.EnumerateByAll(
          languageConfig.createdByInputName,
          languageConfig.createdByName,
          languageConfig.createdByAttributeType,
        )
      case None => CreatedByAttribute.None
    }

    ServiceRoot(
      serviceName,
      module = serviceName,
      comms = serviceBlock.attributes.collect {
        case (_, attr) =>
          attr.attributeType match {
            case ForeignKey(ref) => ref
            case _               => null
          }
      }.toSeq,
      operations = endpoints,
      port = port,
      idAttribute = IDAttribute("id", AttributeType.UUIDType),
      createdByAttribute = createdBy,
      attributes = ListMap.from(serviceBlock.attributes.filter {
        case (_, attr) =>
          attr.attributeType match {
            case _: PrimitiveAttributeType => true
            case _                         => false
          }
      }),
    )
  }
}
