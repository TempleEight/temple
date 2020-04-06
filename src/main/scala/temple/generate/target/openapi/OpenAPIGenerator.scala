package temple.generate.target.openapi

import io.circe.syntax._
import io.circe.yaml.Printer
import temple.ast.AttributeType._
import temple.ast.{Annotation, Attribute}
import temple.collection.FlagMapView
import temple.generate.CRUD._
import temple.generate.FileSystem._
import temple.generate.target.openapi.OpenAPIGenerator._
import temple.generate.target.openapi.ast.OpenAPIFile.{Components, Info}
import temple.generate.target.openapi.ast.OpenAPIType._
import temple.generate.target.openapi.ast.Parameter.InPath
import temple.generate.target.openapi.ast._
import temple.utils.StringUtils

import scala.collection.immutable.ListMap
import scala.collection.mutable

private class OpenAPIGenerator private (name: String, version: String, description: String = "") {

  private val errorTracker = FlagMapView(
    400 -> generateError("Invalid request", "Invalid request parameters: name"),
    401 -> generateError("Valid request but forbidden by server", "Not authorised to create this object"),
    404 -> generateError("ID not found", "Object not found with ID 1"),
    500 -> generateError(
      "The server encountered an error while serving this request",
      "Unable to reach user service: connection timeout",
    ),
  )

  private val paths = mutable.LinkedHashMap[String, Path.Mutable]()

  private def path(url: String): mutable.Map[HTTPVerb, Handler] =
    paths.getOrElseUpdate(url, Path.Mutable()).handlers

  private def pathWithID(url: String, lowerName: String): mutable.Map[HTTPVerb, Handler] = {
    val parameter = Parameter(
      InPath,
      name = "id",
      required = Some(true),
      schema = OpenAPISimpleType("number", "int32"),
      description = s"ID of the $lowerName to perform operations on",
    )
    paths.getOrElseUpdate(url, Path.Mutable(parameters = Seq(parameter))).handlers
  }

  private def isServerAttribute(attribute: Attribute): Boolean = attribute.accessAnnotation contains Annotation.Server

  private def isClientAttribute(attribute: Attribute): Boolean =
    attribute.accessAnnotation.isEmpty || (attribute.accessAnnotation contains Annotation.Client)

  private def attributeToOpenAPIType(attribute: Attribute): OpenAPISimpleType = attribute.attributeType match {
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
    case FloatType(max, min, precision) =>
      val minimum = min.map("minimum" -> _.asJson)
      val maximum = max.map("maximum" -> _.asJson)
      OpenAPISimpleType("number", if (precision > 4) "double" else "float", Seq(minimum, maximum).flatten: _*)
    case ForeignKey(references) =>
      OpenAPISimpleType("number", "int32", "description" -> s"Reference to $references ID".asJson)
  }

  private def generateItemType(attributes: Map[String, Attribute]): OpenAPIObject = OpenAPIObject(
    attributes.iterator
      .filter { case _ -> attribute => !isServerAttribute(attribute) }
      .map { case str -> attribute => str -> attributeToOpenAPIType(attribute) }
      .to(attributes.mapFactory),
  )

  private def generateItemInputType(attributes: Map[String, Attribute]): OpenAPIObject = OpenAPIObject(
    attributes.iterator
      .filter { case _ -> attribute => isClientAttribute(attribute) }
      .map { case str -> attribute => str -> attributeToOpenAPIType(attribute) }
      .to(attributes.mapFactory),
  )

  def addPaths(service: Service): this.type = {
    val lowerName       = service.name.toLowerCase
    val capitalizedName = service.name.capitalize
    val tags            = Seq(capitalizedName)
    service.operations.foreach {
      case List =>
        path(s"/$lowerName/all") += HTTPVerb.Get -> Handler(
            s"Get a list of every $lowerName",
            tags = tags,
            responses = Seq(
              200 -> BodyLiteral(
                jsonContent(MediaTypeObject(OpenAPIArray(generateItemType(service.attributes)))),
                s"$capitalizedName list successfully fetched",
              ),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Create =>
        path(s"/$lowerName") += HTTPVerb.Post -> Handler(
            s"Register a new $lowerName",
            tags = tags,
            requestBody = Some(BodyLiteral(jsonContent(MediaTypeObject(generateItemInputType(service.attributes))))),
            responses = Seq(
              200 -> BodyLiteral(
                jsonContent(MediaTypeObject(generateItemType(service.attributes))),
                s"$capitalizedName successfully created",
              ),
              400 -> Response.Ref(useError(400)),
              401 -> Response.Ref(useError(401)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Read =>
        pathWithID(s"/$lowerName/{id}", lowerName) += HTTPVerb.Get -> Handler(
            s"Look up a single $lowerName",
            tags = tags,
            responses = Seq(
              200 -> BodyLiteral(
                jsonContent(MediaTypeObject(generateItemType(service.attributes))),
                s"$capitalizedName details",
              ),
              400 -> Response.Ref(useError(400)),
              401 -> Response.Ref(useError(401)),
              404 -> Response.Ref(useError(404)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Update =>
        pathWithID(s"/$lowerName/{id}", lowerName) += HTTPVerb.Put -> Handler(
            s"Update a single $lowerName",
            tags = tags,
            requestBody = Some(BodyLiteral(jsonContent(MediaTypeObject(generateItemInputType(service.attributes))))),
            responses = Seq(
              200 -> BodyLiteral(
                jsonContent(MediaTypeObject(generateItemType(service.attributes))),
                s"$capitalizedName successfully updated",
              ),
              400 -> Response.Ref(useError(400)),
              401 -> Response.Ref(useError(401)),
              404 -> Response.Ref(useError(404)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Delete =>
        pathWithID(s"/$lowerName/{id}", lowerName) += HTTPVerb.Delete -> Handler(
            s"Delete a single $lowerName",
            tags = tags,
            responses = Seq(
              200 -> BodyLiteral(
                jsonContent(MediaTypeObject(OpenAPIObject(Map()))),
                s"$capitalizedName successfully deleted",
              ),
              400 -> Response.Ref(useError(400)),
              401 -> Response.Ref(useError(401)),
              404 -> Response.Ref(useError(404)),
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

  def errorBlock: Map[String, Response] = errorTracker.view.map { case i -> response => useError(i) -> response }.toMap

  def toOpenAPI: OpenAPIFile = OpenAPIFile(
    info = Info(name, version, description),
    paths = paths.view.mapValues(_.toPath).to(ListMap),
    components = Components(responses = errorBlock),
  )
}

object OpenAPIGenerator {

  private def jsonContent(mediaTypeObject: MediaTypeObject) = Map("application/json" -> mediaTypeObject)

  private def build(root: OpenAPIRoot): OpenAPIFile = {
    val builder = new OpenAPIGenerator(root.name, root.version, root.description)
    root.services.foreach(builder.addPaths)
    builder.toOpenAPI
  }

  private def render(root: OpenAPIRoot): String =
    Printer(preserveOrder = true, dropNullKeys = true).pretty(build(root).asJson)

  def generate(root: OpenAPIRoot): Files = Map(
    File("api", s"${StringUtils.kebabCase(root.name)}.openapi.yaml") -> render(root),
  )

  /** Create a Response representation for an error */
  private[openapi] def generateError(description: String, example: String): Response =
    BodyLiteral(
      description = description,
      content = jsonContent(
        MediaTypeObject(
          OpenAPIObject(ListMap("error" -> OpenAPISimpleType("string", "example" -> example.asJson))),
        ),
      ),
    )
}
