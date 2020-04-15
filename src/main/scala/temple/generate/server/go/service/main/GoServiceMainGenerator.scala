package temple.generate.server.go.service.main

import temple.ast.AbstractAttribute
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot.ServiceRoot
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
    clientAttributes: ListMap[String, AbstractAttribute],
    operations: Set[CRUD],
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
        when(usesTime) { doubleQuote("time") },
        "",
        when(usesComms) { doubleQuote(s"${root.module}/comm") },
        doubleQuote(s"${root.module}/dao"),
        // TODO: Uncomment when metrics are added to handlers
        //when(usesMetrics) { doubleQuote(s"${root.module}/metric") },
        doubleQuote(s"${root.module}/util"),
        s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
        doubleQuote("github.com/google/uuid"),
        doubleQuote("github.com/gorilla/mux"),
        when(usesMetrics) {
          mkCode.lines(
            // TODO: Uncomment when metrics are added to handlers
            //doubleQuote("github.com/prometheus/client_golang/prometheus"),
            doubleQuote("github.com/prometheus/client_golang/prometheus/promhttp"),
          )
        },
      ),
    )

  private[service] def generateRouter(root: ServiceRoot, operations: Set[CRUD]): String = {
    val handleFuncs =
      for (operation <- operations.toSeq.sorted)
        yield operation match {
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

  private[main] def generateDAOReadInput(root: ServiceRoot): String =
    genDeclareAndAssign(
      genPopulateStruct(s"dao.Read${root.name}Input", ListMap("ID" -> s"${root.decapitalizedName}ID")),
      "input",
    )

  private[main] def generateDAOReadCall(root: ServiceRoot): String =
    genDeclareAndAssign(
      genMethodCall(
        "env.dao",
        s"Read${root.name}",
        "input",
      ),
      root.decapitalizedName,
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
        genReturn(s"${root.decapitalizedName}.CreatedBy == auth.ID", "nil"),
      ),
    )

}
