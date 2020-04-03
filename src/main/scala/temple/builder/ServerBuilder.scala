package temple.builder

import temple.ast.AttributeType.ForeignKey
import temple.ast.Metadata.{Database, ServiceAuth, ServiceEnumerable, ServiceLanguage}
import temple.ast._
import temple.builder.project.LanguageConfig.GoLanguageConfig
import temple.builder.project.ProjectConfig
import temple.detail.LanguageDetail
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.CRUD._
import temple.generate.database.{PostgresContext, PostgresGenerator, PreparedType}
import temple.generate.server._
import temple.utils.StringUtils

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
        )
      case Some(ServiceEnumerable(false)) =>
        CreatedByAttribute.EnumerateByAll(
          languageConfig.createdByInputName,
          languageConfig.createdByName,
        )
      case None => CreatedByAttribute.None
    }

    val idAttribute = IDAttribute("id")

    val queries: ListMap[CRUD, String] =
      DatabaseBuilder
        .buildQuery(serviceName, serviceBlock.attributes, endpoints, createdBy, selectionAttribute = idAttribute.name)
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
      case GoLanguageDetail(modulePath) => s"$modulePath/${StringUtils.kebabCase(serviceName)}"
    }

    // The names of each service this service communicates with, i.e all the foreign key attributes of the service
    val comms: Seq[String] = serviceBlock.attributes.collect {
      case (_, Attribute(x: ForeignKey, _, _)) => x.references
    }.toSeq

    // In the server code all foreign keys are stored as UUID types - map into the correct type
    val attributes: ListMap[String, Attribute] = ListMap.from(serviceBlock.attributes.map {
      case (str, Attribute(_: ForeignKey, access, value)) => (str, Attribute(AttributeType.UUIDType, access, value))
      case default                                        => default
    })

    // TODO: Auth

    ServiceRoot(
      serviceName,
      module = moduleName,
      comms = comms,
      opQueries = queries,
      port = port,
      idAttribute = idAttribute,
      createdByAttribute = createdBy,
      attributes = attributes,
      datastore = serviceBlock.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase),
    )
  }

  def buildAuthRoot(templefile: Templefile, detail: LanguageDetail, port: Int): AuthServiceRoot = {
    val moduleName: String = detail match {
      case GoLanguageDetail(modulePath) => s"$modulePath/auth"
    }

    val serviceAuth = templefile.projectBlock.lookupMetadata[ServiceAuth].getOrElse(ProjectConfig.defaultAuth)

    val attributes: Map[String, Attribute] = serviceAuth match {
      case ServiceAuth.Email =>
        Map(
          "id"       -> Attribute(AttributeType.UUIDType),
          "email"    -> Attribute(AttributeType.StringType()),
          "password" -> Attribute(AttributeType.StringType()),
        )
    }

    val (createQuery: String, readQuery: String) =
      templefile.projectBlock.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase) match {
        case Database.Postgres =>
          implicit val postgresContext: PostgresContext = PostgresContext(PreparedType.DollarNumbers)
          val queries =
            DatabaseBuilder.buildQuery(
              "auth",
              attributes,
              Set(Create, Read),
              CreatedByAttribute.None,
              selectionAttribute = "email",
            )
          val createQuery = PostgresGenerator.generate(queries(Create))
          val readQuery   = PostgresGenerator.generate(queries(Read))
          (createQuery, readQuery)
      }

    AuthServiceRoot(
      moduleName,
      port,
      AuthAttribute(serviceAuth, AttributeType.StringType()),
      IDAttribute("id"),
      createQuery,
      readQuery,
    )
  }
}
