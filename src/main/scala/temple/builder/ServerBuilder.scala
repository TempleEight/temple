package temple.builder

import temple.ast.AbstractAttribute.Attribute
import temple.ast.AttributeType.ForeignKey
import temple.ast.Metadata.{Database, ServiceAuth, ServiceLanguage}
import temple.ast._
import temple.builder.project.LanguageConfig.GoLanguageConfig
import temple.builder.project.ProjectBuilder.endpoints
import temple.builder.project.ProjectConfig
import temple.detail.LanguageDetail
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.CRUD._
import temple.generate.database.{PostgresContext, PostgresGenerator, PreparedType}
import temple.generate.server.AttributesRoot.{ServiceRoot, StructRoot}
import temple.generate.server._
import temple.utils.StringUtils

import scala.Option.when
import scala.collection.immutable.{ListMap, SortedMap}

object ServerBuilder {

  def buildServiceRoot(
    serviceName: String,
    serviceBlock: AbstractServiceBlock,
    port: Int,
    detail: LanguageDetail,
    projectUsesAuth: Boolean,
  ): ServiceRoot = {
    val language = serviceBlock.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    val database = serviceBlock.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase)
    val readable =
      serviceBlock.lookupLocalMetadata[Metadata.Readable].getOrElse(ProjectConfig.getDefaultReadable(projectUsesAuth))
    val writable =
      serviceBlock.lookupLocalMetadata[Metadata.Writable].getOrElse(ProjectConfig.getDefaultWritable(projectUsesAuth))

    val languageConfig = language match {
      case ServiceLanguage.Go => GoLanguageConfig
    }

    // Whether or not this service has an auth block
    val hasAuthBlock = serviceBlock.lookupLocalMetadata[ServiceAuth].isDefined

    val createdBy = when(serviceBlock.attributes.valuesIterator contains AbstractAttribute.CreatedByAttribute) {
      CreatedByAttribute(languageConfig.createdByInputName, languageConfig.createdByName)
    }

    val idAttribute = IDAttribute("id")

    val queries = buildQueries(
      serviceName,
      serviceBlock.storedAttributes,
      endpoints(serviceBlock),
      isStruct = false,
      idAttribute.name,
      database,
      language,
      readable,
    )

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

    val structs = serviceBlock.structs.map {
      case (structName: String, structBlock: StructBlock) =>
        val idAttribute = IDAttribute("id")

        val queries = buildQueries(
          structName,
          structBlock.attributes,
          endpoints(structBlock),
          isStruct = true,
          idAttribute.name,
          database,
          language,
          readable,
        )

        StructRoot(
          name = structName,
          opQueries = queries,
          idAttribute = idAttribute,
          createdByAttribute = None,
          parentAttribute = Some(ParentAttribute("parentID")),
          attributes = ListMap.from(structBlock.providedAttributes),
          readable = readable, // from parent
          writable = writable, // from parent
          projectUsesAuth = projectUsesAuth,
        )
    }

    ServiceRoot(
      serviceName,
      module = moduleName,
      comms = comms.toSet.map(ServiceName(_)),
      opQueries = queries,
      port = port,
      idAttribute = idAttribute,
      createdByAttribute = createdBy,
      attributes = ListMap.from(serviceBlock.providedAttributes),
      structs = structs,
      datastore = serviceBlock.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase),
      readable = readable,
      writable = writable,
      projectUsesAuth = projectUsesAuth,
      hasAuthBlock = hasAuthBlock,
      metrics = serviceBlock.lookupMetadata[Metadata.Metrics],
    )
  }

  private def buildQueries(
    tableName: String,
    attributes: Map[String, AbstractAttribute],
    endpoints: Set[CRUD],
    isStruct: Boolean,
    primaryKey: String,
    database: Database,
    language: ServiceLanguage,
    readable: Metadata.Readable,
  ): SortedMap[CRUD, String] =
    DatabaseBuilder
      .buildQueries(tableName, attributes, endpoints, isStruct, readable, primaryKey)
      .transform {
        case (_, statement) =>
          database match {
            case Database.Postgres =>
              implicit val context: PostgresContext = PostgresContext(ProjectConfig.preparedType(language))
              PostgresGenerator.generate(statement)
          }
      }

  def buildAuthRoot(templefile: Templefile, detail: LanguageDetail, port: Int): AuthServiceRoot = {
    val moduleName: String = detail match {
      case GoLanguageDetail(modulePath) => s"$modulePath/auth"
    }

    // TODO: support usernames
    val authMethod = templefile.lookupMetadata[Metadata.AuthMethod].getOrElse(ProjectConfig.defaultAuth)

    val attributes: Map[String, Attribute] = authMethod match {
      case Metadata.AuthMethod.Email =>
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
            DatabaseBuilder.buildQueries(
              "auth",
              attributes,
              Set(Create, Read),
              isStruct = false,
              selectionAttribute = "email",
            )
          val createQuery = PostgresGenerator.generate(queries(Create))
          val readQuery   = PostgresGenerator.generate(queries(Read))
          (createQuery, readQuery)
      }

    AuthServiceRoot(
      moduleName,
      port,
      AuthAttribute(authMethod, AttributeType.StringType()),
      IDAttribute("id"),
      createQuery,
      readQuery,
      metrics = templefile.projectBlock.lookupMetadata[Metadata.Metrics],
    )
  }
}
