package temple.generate.server.go.service.main

import temple.generate.CRUD._
import temple.generate.server.AttributesRoot
import temple.generate.server.AttributesRoot.{ServiceRoot, StructRoot}
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainGenerator {

  private[service] def generateImports(
    root: ServiceRoot,
    usesBase64: Boolean,
    usesTime: Boolean,
    usesComms: Boolean,
    usesMetrics: Boolean,
  ): String =
    mkCode(
      "import",
      CodeWrap.parens.tabbed(
        when(usesBase64) { doubleQuote("encoding/base64") },
        doubleQuote("encoding/json"),
        doubleQuote("flag"),
        doubleQuote("fmt"),
        doubleQuote("log"),
        doubleQuote("net/http"),
        when(usesMetrics) { doubleQuote("strconv") },
        when(usesTime) { doubleQuote("time") },
        "",
        when(usesComms) { doubleQuote(s"${root.module}/comm") },
        doubleQuote(s"${root.module}/dao"),
        when(usesMetrics) { doubleQuote(s"${root.module}/metric") },
        doubleQuote(s"${root.module}/util"),
        s"valid ${doubleQuote("github.com/go-playground/validator/v10")}",
        doubleQuote("github.com/google/uuid"),
        doubleQuote("github.com/gorilla/mux"),
        when(usesMetrics) {
          mkCode.lines(
            doubleQuote("github.com/prometheus/client_golang/prometheus"),
            doubleQuote("github.com/prometheus/client_golang/prometheus/promhttp"),
          )
        },
      ),
    )

  private[service] def generateRouter(root: ServiceRoot): String = {
    val handleFuncs =
      root.operations.toSeq.map {
        case List =>
          s"r.HandleFunc(${doubleQuote(s"/${root.kebabName}/all")}, env.list${root.name}Handler).Methods(http.MethodGet)"
        case Create =>
          s"r.HandleFunc(${doubleQuote(s"/${root.kebabName}")}, env.create${root.name}Handler).Methods(http.MethodPost)"
        case Read =>
          s"r.HandleFunc(${doubleQuote(s"/${root.kebabName}/{id}")}, env.read${root.name}Handler).Methods(http.MethodGet)"
        case Update =>
          s"r.HandleFunc(${doubleQuote(s"/${root.kebabName}/{id}")}, env.update${root.name}Handler).Methods(http.MethodPut)"
        case Delete =>
          s"r.HandleFunc(${doubleQuote(s"/${root.kebabName}/{id}")}, env.delete${root.name}Handler).Methods(http.MethodDelete)"
        case Identify =>
          s"r.HandleFunc(${doubleQuote(s"/${root.kebabName}")}, env.identify${root.name}Handler).Methods(http.MethodGet)"
      } ++ root.structs.flatMap { struct =>
        val urlPrefix = s"/${root.kebabName}/{parent_id}"
        struct.operations.toSeq.map {
          case List =>
            s"r.HandleFunc(${doubleQuote(s"$urlPrefix/${struct.kebabName}/all")}, env.list${struct.name}Handler).Methods(http.MethodGet)"
          case Create =>
            s"r.HandleFunc(${doubleQuote(s"$urlPrefix/${struct.kebabName}")}, env.create${struct.name}Handler).Methods(http.MethodPost)"
          case Read =>
            s"r.HandleFunc(${doubleQuote(s"$urlPrefix/${struct.kebabName}/{id}")}, env.read${struct.name}Handler).Methods(http.MethodGet)"
          case Update =>
            s"r.HandleFunc(${doubleQuote(s"$urlPrefix/${struct.kebabName}/{id}")}, env.update${struct.name}Handler).Methods(http.MethodPut)"
          case Delete =>
            s"r.HandleFunc(${doubleQuote(s"$urlPrefix/${struct.kebabName}/{id}")}, env.delete${struct.name}Handler).Methods(http.MethodDelete)"
          case Identify =>
            // TODO: can this be prevented at the type level?
            throw new RuntimeException("Cannot identify a struct")
            s"r.HandleFunc(${doubleQuote(s"$urlPrefix/${root.kebabName}")}, env.identify${root.name}Handler).Methods(http.MethodGet)"
        }
      }

    mkCode.lines(
      "// defaultRouter generates a router for this service",
      mkCode(
        "func defaultRouter(env *env) *mux.Router",
        CodeWrap.curly.tabbed(
          genDeclareAndAssign("mux.NewRouter()", "r"),
          "// Mux directs to first matching route, i.e. the order matters",
          handleFuncs,
          "r.Use(jsonMiddleware)",
          genReturn("r"),
        ),
      ),
    )
  }

  private[main] def generateDAOReadInput(block: AttributesRoot): String =
    genDeclareAndAssign(
      genPopulateStruct(s"dao.Read${block.name}Input", ListMap("ID" -> s"${block.decapitalizedName}ID")),
      "input",
    )

  private[main] def generateDAOReadCall(block: AttributesRoot): String =
    genDeclareAndAssign(
      genMethodCall(
        "env.dao",
        s"Read${block.name}",
        "input",
      ),
      block.decapitalizedName,
      "err",
    )

  private[service] def generateCheckAuthorization(root: ServiceRoot): String =
    genFunc(
      "checkAuthorization",
      Seq("env *env", s"${root.decapitalizedName}ID uuid.UUID", "auth *util.Auth"),
      Some(CodeWrap.parens(mkCode.list("bool", "error"))),
      mkCode.lines(
        generateDAOReadInput(root),
        generateDAOReadCall(root),
        genCheckAndReturnError("false"),
        genReturn({
          if (root.hasAuthBlock) s"${root.decapitalizedName}.ID == auth.ID"
          else s"${root.decapitalizedName}.CreatedBy == auth.ID"
        }, "nil"),
      ),
    )

  private[service] def generateCheckParent(struct: StructRoot): String =
    genFunc(
      s"check${struct.name}Parent",
      Seq("env *env", s"${struct.decapitalizedName}ID uuid.UUID", "parentID uuid.UUID"),
      Some(CodeWrap.parens(mkCode.list("bool", "error"))),
      mkCode.lines(
        generateDAOReadInput(struct),
        generateDAOReadCall(struct),
        genCheckAndReturnError("false"),
        genReturn(s"${struct.decapitalizedName}.ParentID == parentID", "nil"),
      ),
    )

}
