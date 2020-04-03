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

import scala.collection.immutable.ListMap

object ServerBuilder {

  def buildServiceRoot(
    serviceName: String,
    serviceBlock: ServiceBlock,
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
      case GoLanguageDetail(modulePath) => s"$modulePath/$serviceName"
    }

    // The names of each service this service communicates with, i.e all the foreign key attributes of the service
    val comms: Seq[String] = serviceBlock.attributes.collect {
      case (_, Attribute(x: ForeignKey, _, _)) => x.references
    }.toSeq

    val readable =
      serviceBlock.lookupLocalMetadata[Metadata.Readable].getOrElse(ProjectConfig.getDefaultReadable(projectUsesAuth))
    val writable =
      serviceBlock.lookupLocalMetadata[Metadata.Writable].getOrElse(ProjectConfig.getDefaultWritable(projectUsesAuth))

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
      readable = readable,
      writable = writable,
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
