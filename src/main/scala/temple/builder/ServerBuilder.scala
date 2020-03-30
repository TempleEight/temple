package temple.builder

import temple.ast.AttributeType.ForeignKey
import temple.ast.Metadata.{Database, ServiceEnumerable, ServiceLanguage}
import temple.ast.{Attribute, AttributeType, Metadata, ServiceBlock}
import temple.builder.project.LanguageConfig.GoLanguageConfig
import temple.builder.project.ProjectConfig
import temple.detail.LanguageDetail
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.CRUD._
import temple.generate.database.{PostgresContext, PostgresGenerator, PreparedType}
import temple.generate.server.{CreatedByAttribute, IDAttribute, ServiceRoot}

import scala.collection.immutable.ListMap

object ServerBuilder {

  def buildServiceRoot(
    serviceName: String,
    serviceBlock: ServiceBlock,
    port: Int,
    endpoints: Set[CRUD],
    detail: LanguageDetail,
  ): ServiceRoot = {
    val language = serviceBlock.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    val database = serviceBlock.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase)
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

    val idAttribute = IDAttribute("id", AttributeType.UUIDType)

    val queries: ListMap[CRUD, String] =
      DatabaseBuilder
        .buildQuery(serviceName, serviceBlock, endpoints, idAttribute, createdBy)
        .map {
          case (crud, statement) =>
            crud -> (database match {
              case Database.Postgres =>
                implicit val context
                  : PostgresContext = PostgresContext(PreparedType.DollarNumbers) //TODO: Does this need to change with server lang?
                PostgresGenerator.generate(statement)
            })
        }

    val moduleName: String = detail match {
      case GoLanguageDetail(modulePath) => s"$modulePath/$serviceName"
    }

    // The names of each service this service communicates with, i.e all the foreign key attributes of the service
    val comms: Seq[String] = serviceBlock.attributes.collect {
      case (_, Attribute(x: ForeignKey, _, _)) => x.references
    }.toSeq

    // TODO: Auth

    ServiceRoot(
      serviceName,
      module = moduleName,
      comms = comms,
      opQueries = queries,
      port = port,
      idAttribute = idAttribute,
      createdByAttribute = createdBy,
      attributes = ListMap.from(serviceBlock.attributes),
      datastore = serviceBlock.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase),
    )
  }
}
