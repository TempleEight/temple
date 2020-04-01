package temple.generate.server.go.service.main

import temple.ast.{Attribute, AttributeType}
import temple.generate.CRUD
import temple.generate.CRUD._
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainGenerator {

  private[service] def generateImports(
    root: ServiceRoot,
    usesTime: Boolean,
    usesComms: Boolean,
    clientAttributes: ListMap[String, Attribute],
  ): String =
    mkCode(
      "import",
      CodeWrap.parens.tabbed(
        //doubleQuote("encoding/json"),
        //doubleQuote("flag"),
        //doubleQuote("fmt"),
        //doubleQuote("log"),
        doubleQuote("net/http"),
        // TODO: This check is temporary to make the integration tests pass
        when(
          clientAttributes
            .exists {
              case (_, attr) =>
                attr.attributeType == AttributeType.DateType ||
                attr.attributeType == AttributeType.TimeType ||
                attr.attributeType == AttributeType.DateTimeType
            },
        ) { doubleQuote("time") },
        //when(usesTime) { doubleQuote("time") },
        "",
        when(usesComms) { doubleQuote(s"${root.module}/comm") },
        doubleQuote(s"${root.module}/dao"),
        //doubleQuote(s"${root.module}/util"),
        //s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
        doubleQuote("github.com/google/uuid"),
        doubleQuote("github.com/gorilla/mux"),
      ),
    )

  private[service] def generateRouter(root: ServiceRoot, operations: Set[CRUD]): String = {
    val handleFuncs =
      for (operation <- operations.toSeq.sorted)
        yield operation match {
          case List =>
            s"r.HandleFunc(${doubleQuote(s"/${root.name}/all")}, env.list${root.name.capitalize}Handler).Methods(http.MethodGet)"
          case Create =>
            s"r.HandleFunc(${doubleQuote(s"/${root.name}")}, env.create${root.name.capitalize}Handler).Methods(http.MethodPost)"
          case Read =>
            s"r.HandleFunc(${doubleQuote(s"/${root.name}/{id}")}, env.read${root.name.capitalize}Handler).Methods(http.MethodGet)"
          case Update =>
            s"r.HandleFunc(${doubleQuote(s"/${root.name}/{id}")}, env.update${root.name.capitalize}Handler).Methods(http.MethodPut)"
          case Delete =>
            s"r.HandleFunc(${doubleQuote(s"/${root.name}/{id}")}, env.delete${root.name.capitalize}Handler).Methods(http.MethodDelete)"
        }

    mkCode.lines(
      "// router generates a router for this service",
      mkCode(
        "func (env *env) router() *mux.Router",
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

  /** Given a service name, whether the service uses inter-service communication, the operations desired and the port
    * number, generate the main function */
  private[service] def generateMain(root: ServiceRoot, usesComms: Boolean, operations: Set[CRUD]): String =
    mkCode(
      "func main() ",
      CodeWrap.curly.tabbed(
        mkCode.lines(
          "",
          /*
          s"""configPtr := flag.String("config", "/etc/$serviceName-service/config.json", "configuration filepath")""",
          "flag.Parse()",
          "",
          "// Require all struct fields by default",
          "valid.SetFieldsRequiredByDefault(true)",
          "",
          "config, err := util.GetConfig(*configPtr)",
          "if err != nil " + CodeWrap.curly.tabbed("log.Fatal(err)"),
          "",
          s"dao = ${serviceName}DAO.DAO{}",
          "err = dao.Init(config)",
          "if err != nil " + CodeWrap.curly.tabbed("log.Fatal(err)"),
          "",
          when(usesComms) {
            mkCode.lines(
              s"comm = ${serviceName}Comm.Handler{}",
              "comm.Init(config)",
              "",
            )
          },
          "r := mux.NewRouter()",
          when(operations.contains(CRUD.List)) {
            mkCode.lines(
              "// Mux directs to first matching route, i.e. the order matters",
              s"""r.HandleFunc("/$serviceName/all", ${serviceName}ListHandler).Methods(http.MethodGet)""",
            )
          },
          when(operations.contains(CRUD.Create)) {
            s"""r.HandleFunc("/$serviceName", ${serviceName}CreateHandler).Methods(http.MethodPost)"""
          },
          when(operations.contains(CRUD.Read)) {
            s"""r.HandleFunc("/$serviceName/{id}", ${serviceName}ReadHandler).Methods(http.MethodGet)"""
          },
          when(operations.contains(CRUD.Update)) {
            s"""r.HandleFunc("/$serviceName/{id}", ${serviceName}UpdateHandler).Methods(http.MethodPut)"""
          },
          when(operations.contains(CRUD.Delete)) {
            s"""r.HandleFunc("/$serviceName/{id}", ${serviceName}DeleteHandler).Methods(http.MethodDelete)"""
          },
          "r.Use(jsonMiddleware)",
          "",
          s"""log.Fatal(http.ListenAndServe(":$port", r))""",
         */
        ),
      ),
    )

  private def generateHandler(root: ServiceRoot, operation: CRUD): String =
    s"func (env *env) ${operation.toString.toLowerCase}${root.name.capitalize}Handler(w http.ResponseWriter, r *http.Request) {}"

  private[service] def generateHandlers(root: ServiceRoot, operations: Set[CRUD]): String =
    mkCode.doubleLines(
      for (operation <- operations.toSeq.sorted)
        yield generateHandler(root, operation),
    )
}
