package temple.generate.docs

import io.github.swagger2markup.markup.builder.{MarkupDocBuilder, MarkupDocBuilders, MarkupLanguage, MarkupTableColumn}
import temple.ast.{AbstractServiceBlock, AttributeBlock, Templefile}
import temple.builder.project.ProjectBuilder
import temple.generate.CRUD
import temple.generate.FileSystem.{File, Files}
import temple.utils.StringUtils
import temple.generate.docs.internal.DocumentationGeneratorUtils.{description, url, _}
import io.circe.syntax._
import temple.ast.Metadata.Provider

import Option.when
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

  private def generateDocs(
    builder: MarkupDocBuilder,
    name: String,
    block: AttributeBlock[_],
    usesAuth: Boolean,
    structName: Option[String] = None,
  ): Unit = {
    val kebabName          = StringUtils.kebabCase(name)
    val lowerName          = name.toLowerCase
    val requestAttributes  = block.attributes.filter { _._2.inRequest }
    val responseAttributes = block.attributes.filter { _._2.inResponse }
    val mockResponse       = generateResponseBody(responseAttributes)

    if (structName.isEmpty) builder.sectionTitleLevel2(s"$name Service")
    ProjectBuilder.endpoints(block).foreach { endpoint =>
      builder.sectionTitleLevel3(s"${endpoint.toString} ${structName.getOrElse(name)}")
      addGeneralInfo(
        builder,
        url(kebabName, endpoint, structName.map(StringUtils.kebabCase)),
        httpMethod(endpoint),
        description(lowerName, endpoint, structName.map(_.toLowerCase)),
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

  def generateFileContentDescription(templefile: Templefile): Seq[(String, String)] = {
    val baseFolderDefinitions = Seq(
      "/api" -> "API contains the OpenAPI schema for the project",
    )

    val metricsDefinitions = if (templefile.usesMetrics) {
      Seq(
        "/grafana"    -> "Grafana configuration providing a dashboard for each service in the project",
        "/prometheus" -> "Prometheus configuration for each service in the project",
      )
    } else Seq()

    val providerDefinitions = templefile
      .lookupMetadata[Provider]
      .map { provider =>
        val providerSpecific = provider match {
          case Provider.Kubernetes =>
            Seq(
              "/kube"         -> "YAML files for deploying the project with Kubernetes",
              "push-image.sh" -> "Deployment script to push each script to a locally hosted registry",
            )
          case Provider.DockerCompose =>
            Seq("docker-compose.yml" -> "Configuration for deploying the project with Docker Compose")
        }
        providerSpecific ++ Seq(
          "/kong"     -> "Configuration for the Kong API Gateway",
          "deploy.sh" -> "Deployment script for local development (must be used with `source`)",
        )
      }
      .getOrElse(Seq())

    val serviceDefinitions = templefile.allServices.keys.toSeq.sorted.flatMap { service =>
      Seq(
        s"/${StringUtils.kebabCase(service)}"    -> s"Backend code for the $service service",
        s"/${StringUtils.kebabCase(service)}-db" -> s"Initialisation scripts for the $service database",
      )
    }

    baseFolderDefinitions ++ metricsDefinitions ++ providerDefinitions ++ serviceDefinitions
  }

  def generate(templefile: Templefile): Files = {
    val builder = MarkupDocBuilders.documentBuilder(MarkupLanguage.MARKDOWN)
    builder
      .documentTitle(s"${templefile.projectName}")
      .textLine("This directory contains files generated by [Temple](https://templeeight.github.io/temple-docs)", true)

    builder.sectionTitleLevel1("Project Structure")
    val fileDesc = generateFileContentDescription(templefile)
    fileDesc.sorted.foreach {
      case (name, description) =>
        builder.unorderedListItem(s"`$name`: $description")
    }

    builder.sectionTitleLevel1("API Documentation")

    if (templefile.usesAuth) {
      generateAuthServiceDocs(builder)
    }

    templefile.providedServices.foreach {
      case (name, service) =>
        generateDocs(builder, name, service, templefile.usesAuth)
        service.structs.foreach {
          case (structName, struct) =>
            generateDocs(builder, name, struct, templefile.usesAuth, Some(structName))
        }
    }

    Map(File("", "README.md") -> builder.toString)
  }
}
