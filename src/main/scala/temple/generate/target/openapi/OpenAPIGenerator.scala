package temple.generate.target.openapi

import io.circe.syntax._
import io.circe.yaml.Printer
import temple.DSL.semantics.AttributeType._
import temple.DSL.semantics.{Annotation, Attribute}
import temple.collection.FlagMapView
import temple.generate.Crud._
import temple.generate.target.openapi.OpenAPIFile.{Components, Info}
import temple.generate.target.openapi.OpenAPIGenerator._
import temple.generate.target.openapi.OpenAPIType._
import temple.generate.target.openapi.Parameter.InPath

import scala.collection.immutable.ListMap
import scala.collection.mutable

private class OpenAPIGenerator private (name: String, version: String, description: String = "") {

  private val errorTracker = FlagMapView(
    400 -> generateError("Invalid request", "Invalid request parameters: name"),
    404 -> generateError("ID not found", "Object not found with ID 1"),
    500 -> generateError(
      "The server encountered an error while serving this request",
      "Unable to reach user service: connection timeout",
    ),
  )

  private val paths = mutable.Map[String, mutable.Map[HTTPVerb, Handler]]()

  private def path(url: String): mutable.Map[HTTPVerb, Handler] =
    paths.getOrElseUpdate(url, mutable.Map())

  private def isServerAttribute(attribute: Attribute): Boolean = attribute.accessAnnotation contains Annotation.Server

  private def isClientAttribute(attribute: Attribute): Boolean =
    attribute.accessAnnotation.isEmpty || (attribute.accessAnnotation contains Annotation.Client)

  private def attributeToOpenAPIType(attribute: Attribute): OpenAPISimpleType = attribute.attributeType match {
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
      case ReadAll =>
        path(s"/$lowerName/all") += HTTPVerb.Get -> Handler(
            s"Get a list of every $lowerName",
            tags = tags,
            responses = Map(
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
            responses = Map(
              200 -> BodyLiteral(
                jsonContent(MediaTypeObject(generateItemType(service.attributes))),
                s"$capitalizedName successfully created",
              ),
              400 -> Response.Ref(useError(400)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Read =>
        path(s"/$lowerName/{id}") += HTTPVerb.Get -> Handler(
            s"Look up a single $lowerName",
            tags = tags,
            parameters = Seq(
              Parameter(
                InPath,
                name = "id",
                required = Some(true),
                schema = OpenAPISimpleType("number", "int32"),
                description = s"ID of the $lowerName to get",
              ),
            ),
            responses = Map(
              200 -> BodyLiteral(
                jsonContent(MediaTypeObject(generateItemType(service.attributes))),
                s"$capitalizedName details",
              ),
              400 -> Response.Ref(useError(400)),
              500 -> Response.Ref(useError(500)),
            ),
          )
      case Update => // TODO in future PR
      case Delete => // TODO in future PR
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
    paths = paths.view.mapValues(_.toMap).toMap,
    components = Components(responses = errorBlock),
  )
}

object OpenAPIGenerator {

  private def build(name: String, version: String, description: String = "")(services: Service*): OpenAPIFile = {
    val builder = new OpenAPIGenerator(name, version, description)
    services.foreach(builder.addPaths)
    builder.toOpenAPI
  }

  def render(name: String, version: String, description: String = "")(services: Service*): String =
    Printer(preserveOrder = true, dropNullKeys = true).pretty(build(name, version, description)(services: _*).asJson)

  private def jsonContent(mediaTypeObject: MediaTypeObject) = Map("application/json" -> mediaTypeObject)

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
