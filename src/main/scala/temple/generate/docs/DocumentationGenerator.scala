package temple.generate.docs

import io.github.swagger2markup.markup.builder.{MarkupDocBuilder, MarkupDocBuilders, MarkupLanguage, MarkupTableColumn}
import temple.ast.{AbstractServiceBlock, Templefile}
import temple.builder.project.ProjectBuilder
import temple.generate.CRUD
import temple.generate.FileSystem.{File, Files}
import temple.utils.StringUtils
import temple.generate.docs.internal.DocumentationGeneratorUtils.{description, url, _}

import io.circe.syntax._
import scala.jdk.CollectionConverters._

object DocumentationGenerator {

  // Add general information about the endpoint, including URL, method, description & auth if used
  private def addGeneralInfo(
    builder: MarkupDocBuilder,
    url: String,
    method: String,
    description: String,
    usesAuth: Boolean,
  ): Unit = {
    // Have to add whitespace manually :'(
    builder.boldText("URL:").text(" ").literalTextLine(url, true)
    builder.boldText("Method:").text(" ").literalTextLine(method, true)
    builder.boldText("Description:").text(" ").textLine(description, true)
    if (usesAuth) {
      builder
        .boldText("Auth:")
        .text(" ")
        .text("Include Authorization header in format")
        .text(" ")
        .literalTextLine("Authorization: Bearer <token>", true)
    }
  }

  private def addRequestContentTable(builder: MarkupDocBuilder, contents: Seq[Seq[String]]): Unit =
    builder
      .boldTextLine("Request Contents:", true)
      .tableWithColumnSpecs(
        Seq("Parameter", "Type", "Details").map(new MarkupTableColumn(_)).asJava,
        contents.map(_.asJava).asJava,
      )

  private def addJSONSuccessResponse(builder: MarkupDocBuilder, code: String, json: String): Unit = {
    builder.sectionTitleLevel4("Success Response:")
    builder.boldText("Code:").text(" ").literalTextLine(code, true)
    builder
      .boldTextLine("Response Body:", true)
      .listingBlock(json)
  }

  private def addHeaderSuccessResponse(builder: MarkupDocBuilder, code: String, header: String): Unit = {
    builder.sectionTitleLevel4("Success Response:")
    builder.boldText("Code:").text(" ").literalTextLine(code, true)
    builder
      .boldText("Headers:")
      .literalTextLine(header, true)
  }

  // Add a list of error responses
  private def addErrorResponses(builder: MarkupDocBuilder, codes: Seq[Int]): Unit = {
    builder.sectionTitleLevel4("Error Responses:")
    codes.map { code =>
      builder
        .boldText("Code:")
        .text(" ")
        .literalTextLine(httpCodeString(code), true)
    }
  }

  private def generateServiceDocs(
    builder: MarkupDocBuilder,
    name: String,
    service: AbstractServiceBlock,
    usesAuth: Boolean,
  ): Unit = {
    val kebabName          = StringUtils.kebabCase(name)
    val lowerName          = name.toLowerCase
    val requestAttributes  = service.attributes.filter { _._2.inRequest }
    val responseAttributes = service.attributes.filter { _._2.inResponse }
    val mockResponse       = generateResponseBody(responseAttributes)

    builder.sectionTitleLevel2(s"$name Service")
    ProjectBuilder.endpoints(service).foreach { endpoint =>
      builder.sectionTitleLevel3(s"${endpoint.toString} $name")
      addGeneralInfo(
        builder,
        url(kebabName, endpoint),
        httpMethod(endpoint),
        description(lowerName, endpoint),
        usesAuth,
      )

      // Add the request body
      if ((endpoint == CRUD.Create || endpoint == CRUD.Update) && requestAttributes.nonEmpty) {
        addRequestContentTable(builder, generateRequestBody(requestAttributes))
      }

      endpoint match {
        case CRUD.List =>
          addJSONSuccessResponse(builder, httpCodeString(200), Map(s"${name}List" -> Seq(mockResponse)).asJson.spaces2)
        case CRUD.Create | CRUD.Read | CRUD.Update =>
          addJSONSuccessResponse(builder, httpCodeString(200), mockResponse.asJson.spaces2)
        case CRUD.Delete =>
          addJSONSuccessResponse(builder, httpCodeString(200), "{}")
        case CRUD.Identify =>
          addHeaderSuccessResponse(builder, httpCodeString(302), "Location: http://path/to/resource")
      }
      addErrorResponses(builder, errorCodes(endpoint, usesAuth).toSeq.sorted)
      builder.newLine().pageBreak()
    }
  }

  private def generateAuthServiceDocs(builder: MarkupDocBuilder): Unit = {
    builder.sectionTitleLevel2("Auth")
    Seq(("register", Seq(400, 403, 500)), ("login", Seq(400, 401, 500))).foreach {
      case (endpoint, errorCodes) =>
        builder.sectionTitleLevel3(endpoint.capitalize)

        addGeneralInfo(
          builder,
          s"/api/$endpoint",
          "POST",
          s"${endpoint.capitalize} and get an access token",
          usesAuth = false,
        )

        addRequestContentTable(
          builder,
          Seq(
            Seq("email", "String", "Valid email address"),
            Seq("password", "String", "Between 8 and 64 characters"),
          ),
        )

        addJSONSuccessResponse(builder, httpCodeString(200), Map("AccessToken" -> "").asJson.spaces2)
        addErrorResponses(builder, errorCodes)

        builder.newLine().pageBreak()
    }
  }

  def generate(templefile: Templefile): Files = {
    val builder = MarkupDocBuilders.documentBuilder(MarkupLanguage.MARKDOWN)
    builder
      .documentTitle(s"${templefile.projectName}")
      .sectionTitleLevel1("API Documentation")

    if (templefile.usesAuth) {
      generateAuthServiceDocs(builder)
    }

    templefile.providedServices.foreach {
      case (name, service) =>
        generateServiceDocs(builder, name, service, templefile.usesAuth)
    }

    Map(File("", "README.md") -> builder.toString)
  }
}
