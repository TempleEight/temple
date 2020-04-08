package temple.builder

import temple.ast.AbstractAttribute.Attribute
import temple.ast.AttributeType.ForeignKey
import temple.ast.Metadata.{Database, ServiceAuth, ServiceLanguage}
import temple.ast.{Metadata, _}
import temple.builder.project.LanguageConfig.GoLanguageConfig
import temple.builder.project.ProjectConfig
import temple.detail.LanguageDetail
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.CRUD._
import temple.generate.database.{PostgresContext, PostgresGenerator, PreparedType}
import temple.generate.server
import temple.generate.server._
import temple.utils.StringUtils

import scala.collection.immutable.ListMap

object ServerBuilder {

  def buildServiceRoot(
    serviceName: String,
    serviceBlock: AbstractServiceBlock,
    port: Int,
    endpoints: Set[CRUD],
    detail: LanguageDetail,
    projectUsesAuth: Boolean,
  ): ServiceRoot = {
    val language = serviceBlock.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    val database = serviceBlock.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase)
    val languageConfig = language match {
      case ServiceLanguage.Go => GoLanguageConfig
    }

    // Whether or not this service has an auth block
    val hasAuthBlock = serviceBlock.lookupLocalMetadata[ServiceAuth].isDefined

    // The created by attribute exists if the project has auth and this service does not have an auth block
    val createdBy =
      if (projectUsesAuth && !hasAuthBlock)
        Some(CreatedByAttribute(languageConfig.createdByInputName, languageConfig.createdByName))
      else None

    val idAttribute = server.IDAttribute("id")

    val readable =
      serviceBlock.lookupLocalMetadata[Metadata.Readable].getOrElse(ProjectConfig.getDefaultReadable(projectUsesAuth))
    val writable =
      serviceBlock.lookupLocalMetadata[Metadata.Writable].getOrElse(ProjectConfig.getDefaultWritable(projectUsesAuth))

    val queries: ListMap[CRUD, String] =
      DatabaseBuilder
        .buildQuery(serviceName, serviceBlock.attributes, endpoints, readable, selectionAttribute = idAttribute.name)
        .map {
          case (crud, statement) =>
            crud -> (database match {
              case Database.Postgres =>
                //TODO: Does this need to change with server lang?
                implicit val context: PostgresContext = PostgresContext(PreparedType.DollarNumbers)
                PostgresGenerator.generate(statement)
            })
        }

    val moduleName: String = detail match {
      case GoLanguageDetail(modulePath) => s"$modulePath/${StringUtils.kebabCase(serviceName)}"
    }

    // The names of each service this service communicates with, i.e all the foreign key attributes of the service and any inner structs
    // TODO: Does this work for service referencing the struct? Is that allowed?
    val comms = serviceBlock.attributes.collect {
        case (_, Attribute(x: ForeignKey, _, _)) => x.references
      } ++ serviceBlock.structs.values
        .flatMap { block =>
          block.attributes.collect {
            case (_, Attribute(x: ForeignKey, _, _)) if x.references != serviceName => x.references
          }
        }

    ServiceRoot(
      serviceName,
      module = moduleName,
      comms = comms.toSeq,
      opQueries = queries,
      port = port,
      idAttribute = idAttribute,
      createdByAttribute = createdBy,
      attributes = ListMap.from(serviceBlock.providedAttributes),
      datastore = serviceBlock.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase),
      readable = readable,
      writable = writable,
      projectUsesAuth = projectUsesAuth,
      hasAuthBlock = hasAuthBlock,
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
      server.IDAttribute("id"),
      createQuery,
      readQuery,
    )
  }
}
