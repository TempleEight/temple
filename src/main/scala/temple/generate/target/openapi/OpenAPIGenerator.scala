package temple.generate.target.openapi

import io.circe.syntax._
import io.circe.yaml.Printer
import temple.ast.AttributeType._
import temple.ast.{AbstractAttribute, Annotation}
import temple.collection.FlagMapView
import temple.generate.CRUD._
import temple.generate.FileSystem._
import temple.generate.target.openapi.OpenAPIGenerator._
import temple.generate.target.openapi.ast.OpenAPIFile.{Components, Info}
import temple.generate.target.openapi.ast.OpenAPIType._
import temple.generate.target.openapi.ast.Parameter.InPath
import temple.generate.target.openapi.ast._
import temple.utils.StringUtils
import Option.when

import scala.collection.immutable.ListMap
import scala.collection.mutable

private class OpenAPIGenerator private (
  name: String,
  version: String,
  description: String = "",
  securityScheme: Option[(String, SecurityScheme)] = None,
) {

  private val errorTracker = FlagMapView(
    400 -> generateError("Invalid request", "Invalid request parameters: name"),
    401 -> generateError("Valid request but forbidden by server", "Not authorised to create this object"),
    403 -> generateError("Valid request but server will not fulfill", "User with this ID already exists"),
    404 -> generateError("ID not found", "Object not found with ID 1"),
    500 -> generateError(
      "The server encountered an error while serving this request",
      "Unable to reach user service: connection timeout",
    ),
  )

  private val paths = mutable.LinkedHashMap[String, Path.Mutable]()

  private def path(url: String, lowerName: String): mutable.Map[HTTPVerb, Handler] = {
    val idParameter = when(url.contains("{id}")) {
      Parameter(
        InPath,
        name = "id",
        required = Some(true),
        schema = OpenAPISimpleType("string", "uuid"),
        description = s"ID of the $lowerName to perform operations on",
      )
    }

    val parentIDParameter = when(url.contains("{parent_id}")) {
      Parameter(
        InPath,
        name = "parent_id",
        required = Some(true),
        schema = OpenAPISimpleType("string", "uuid"),
        description = s"ID of the parent which owns this entity",
      )
    }

    paths.getOrElseUpdate(url, Path.Mutable(parameters = Seq(idParameter, parentIDParameter).flatten)).handlers
  }

  private def attributeToOpenAPIType(attribute: AbstractAttribute): OpenAPISimpleType = attribute.attributeType match {
    case UUIDType     => OpenAPISimpleType("string", "uuid")
    case BoolType     => OpenAPISimpleType("boolean")
    case DateType     => OpenAPISimpleType("string", "date")
    case DateTimeType => OpenAPISimpleType("string", "date-time")
    case TimeType     => OpenAPISimpleType("string", "time")
    case BlobType(size) =>
      val maxLength = size.map("maxLength" -> _.asJson)
      OpenAPISimpleType("string", Seq(maxLength).flatten: _*)
    case StringType(max, min) =>
      val minLength = min.map("minLength" -> _.asJson)
      val maxLength = max.map("maxLength" -> _.asJson)
      OpenAPISimpleType("string", Seq(minLength, maxLength).flatten: _*)
    case IntType(max, min, precision) =>
      val minimum = min.map("minimum" -> _.asJson)
      val maximum = max.map("maximum" -> _.asJson)
      OpenAPISimpleType("number", if (precision > 4) "int64" else "int32", Seq(minimum, maximum).flatten: _*)
    case floatType @ FloatType(max, min, _) =>
      val minimum = min.map("minimum" -> _.asJson)
      val maximum = max.map("maximum" -> _.asJson)
      OpenAPISimpleType("number", if (floatType.isDouble) "double" else "float", Seq(minimum, maximum).flatten: _*)
    case ForeignKey(references) =>
      OpenAPISimpleType("number", "int32", "description" -> s"Reference to $references ID".asJson)
  }

  private def generateItemType(attributes: Map[String, AbstractAttribute]): OpenAPIObject = OpenAPIObject(
    attributes.iterator
      .filter { case _ -> attribute => attribute.inResponse }
      .map { case str -> attribute => str -> attributeToOpenAPIType(attribute) }
      .to(attributes.mapFactory),
  )

  private def generateItemInputType(attributes: Map[String, AbstractAttribute]): OpenAPIObject = OpenAPIObject(
    attributes.iterator
      .filter { case _ -> attribute => attribute.inRequest }
      .map { case str -> attribute => str -> attributeToOpenAPIType(attribute) }
      .to(attributes.mapFactory),
  )

  def addPaths(prefix: String, service: AbstractService): this.type = {
    val securitySchemeName = securityScheme.map { case (name, _) => name }
    val lowerName          = service.name.toLowerCase
    val capitalizedName    = service.name.capitalize
    val tags               = Seq(capitalizedName)
    service.operations.foreach {
      case List =>
        path(s"$prefix/all", lowerName) += HTTPVerb.Get -> Handler(
            s"Get a list of every $lowerName",
            tags = tags,
            security = securitySchemeName,
            responses = Seq(
              200 -> ResponseObject(
                s"$capitalizedName list successfully fetched",
                Some(jsonContent(MediaTypeObject(OpenAPIArray(generateItemType(service.attributes))))),
              ),
              401 -> Response.Ref(useError(401)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Create =>
        path(s"$prefix", lowerName) += HTTPVerb.Post -> Handler(
            s"Register a new $lowerName",
            tags = tags,
            security = securitySchemeName,
            requestBody =
              Some(RequestBodyObject(jsonContent(MediaTypeObject(generateItemInputType(service.attributes))))),
            responses = Seq(
              200 -> ResponseObject(
                s"$capitalizedName successfully created",
                Some(jsonContent(MediaTypeObject(generateItemType(service.attributes)))),
              ),
              400 -> Response.Ref(useError(400)),
              401 -> Response.Ref(useError(401)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Read =>
        path(s"$prefix/{id}", lowerName) += HTTPVerb.Get -> Handler(
            s"Look up a single $lowerName",
            tags = tags,
            security = securitySchemeName,
            responses = Seq(
              200 -> ResponseObject(
                s"$capitalizedName details",
                Some(jsonContent(MediaTypeObject(generateItemType(service.attributes)))),
              ),
              400 -> Response.Ref(useError(400)),
              401 -> Response.Ref(useError(401)),
              404 -> Response.Ref(useError(404)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Update =>
        path(s"$prefix/{id}", lowerName) += HTTPVerb.Put -> Handler(
            s"Update a single $lowerName",
            tags = tags,
            security = securitySchemeName,
            requestBody =
              Some(RequestBodyObject(jsonContent(MediaTypeObject(generateItemInputType(service.attributes))))),
            responses = Seq(
              200 -> ResponseObject(
                s"$capitalizedName successfully updated",
                Some(jsonContent(MediaTypeObject(generateItemType(service.attributes)))),
              ),
              400 -> Response.Ref(useError(400)),
              401 -> Response.Ref(useError(401)),
              404 -> Response.Ref(useError(404)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Delete =>
        path(s"$prefix/{id}", lowerName) += HTTPVerb.Delete -> Handler(
            s"Delete a single $lowerName",
            tags = tags,
            security = securitySchemeName,
            responses = Seq(
              200 -> ResponseObject(
                s"$capitalizedName successfully deleted",
                Some(jsonContent(MediaTypeObject(OpenAPIObject(Map())))),
              ),
              400 -> Response.Ref(useError(400)),
              401 -> Response.Ref(useError(401)),
              404 -> Response.Ref(useError(404)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Identify =>
        path(s"$prefix", lowerName) += HTTPVerb.Get -> Handler(
            s"Look up the single $lowerName associated with the access token",
            tags = tags,
            security = securitySchemeName,
            responses = Seq(
              302 -> ResponseObject(
                description = s"The single $lowerName is accessible from the provided Location",
                content = None,
                headers = Some(
                  Map(
                    "Location" -> HeaderObject(
                      description = s"The location where the single $lowerName can be found",
                      typ = "string",
                    ),
                  ),
                ),
              ),
              404 -> Response.Ref(useError(404)),
              500 -> Response.Ref(useError(500)),
            ),
          )
    }
    this
  }

  def addAuthPaths(auth: Auth): this.type = {
    val tags = Seq("Auth")

    auth match {
      case Auth.Email =>
        val requestBody = Map(
          "Email" -> OpenAPISimpleType("string", "email"),
          "Password" -> OpenAPISimpleType(
            "string",
            "password",
            "minLength" -> 8.asJson,
            "maxLength" -> 64.asJson,
          ),
        )

        val responseBody = OpenAPIObject(Map("AccessToken" -> OpenAPISimpleType("string")))

        path(s"/auth/register", "auth") += HTTPVerb.Post -> Handler(
          s"Register and get an access token",
          tags = tags,
          requestBody = Some(RequestBodyObject(jsonContent(MediaTypeObject(OpenAPIObject(requestBody))))),
          responses = Seq(
            200 -> ResponseObject(
              s"Successful registration",
              Some(jsonContent(MediaTypeObject(responseBody))),
            ),
            400 -> Response.Ref(useError(400)),
            403 -> Response.Ref(useError(403)),
            500 -> Response.Ref(useError(500)),
          ),
        )
        path(s"/auth/login", "auth") += HTTPVerb.Post -> Handler(
          s"Login and get an access token",
          tags = tags,
          requestBody = Some(RequestBodyObject(jsonContent(MediaTypeObject(OpenAPIObject(requestBody))))),
          responses = Seq(
            200 -> ResponseObject(
              s"Successful login",
              Some(jsonContent(MediaTypeObject(responseBody))),
            ),
            400 -> Response.Ref(useError(400)),
            401 -> Response.Ref(useError(401)),
            500 -> Response.Ref(useError(500)),
          ),
        )
    }
    this
  }

  def useError(code: Int): String = {
    errorTracker.flag(code)
    s"Error$code"
  }

  def errorBlock: Map[String, Response] =
    errorTracker.view.map { case i -> response => useError(i) -> response }.toSeq.sortBy(_._1).to(ListMap)

  def toOpenAPI: OpenAPIFile = OpenAPIFile(
    info = Info(name, version, description),
    paths = paths.view.mapValues(_.toPath).to(ListMap),
    components = Components(securitySchemes = securityScheme.toMap, responses = errorBlock),
  )
}

object OpenAPIGenerator {

  private def jsonContent(mediaTypeObject: MediaTypeObject) = Map("application/json" -> mediaTypeObject)

  private def build(root: OpenAPIRoot): OpenAPIFile = {
    val securityScheme = root.auth.map {
      case Auth.Email => "bearerAuth" -> SecurityScheme("http", "bearer", "JWT")
    }
    val builder = new OpenAPIGenerator(root.name, root.version, root.description, securityScheme)

    root.services.foreach { service =>
      val kebabName = StringUtils.kebabCase(service.name)
      builder.addPaths(s"/$kebabName", service)

      service.structs.foreach { struct =>
        val kebabStructName = StringUtils.kebabCase(struct.name)
        builder.addPaths(s"/$kebabName/{parent_id}/$kebabStructName", struct)
      }
    }

    root.auth.foreach(builder.addAuthPaths)
    builder.toOpenAPI
  }

  private def render(root: OpenAPIRoot): String =
    Printer(preserveOrder = true, dropNullKeys = true).pretty(build(root).asJson)

  def generate(root: OpenAPIRoot): Files = Map(
    File("api", s"${StringUtils.kebabCase(root.name)}.openapi.yaml") -> render(root),
  )

  /** Create a Response representation for an error */
  private[openapi] def generateError(description: String, example: String): Response =
    ResponseObject(
      description = description,
      content = Some(
        jsonContent(
          MediaTypeObject(
            OpenAPIObject(ListMap("error" -> OpenAPISimpleType("string", "example" -> example.asJson))),
          ),
        ),
      ),
    )
}
