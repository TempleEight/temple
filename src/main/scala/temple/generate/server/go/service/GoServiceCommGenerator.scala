package temple.generate.server.go.service

import temple.ast.AttributeType
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceCommGenerator {

  private[service] def generateImports(root: ServiceRoot): String =
    mkCode(
      "import",
      CodeWrap.parens.tabbed(
        doubleQuote("errors"),
        doubleQuote("fmt"),
        doubleQuote("net/http"),
        "",
        doubleQuote(s"${root.module}/util"),
        doubleQuote("github.com/google/uuid"),
      ),
    )

  private[service] def generateCommFunctionDecl(root: ServiceRoot, serviceName: String): String =
    mkCode(
      genFunctionCall(
        s"Check${serviceName.capitalize}",
        s"${serviceName}${root.idAttribute.name.toUpperCase} ${generateGoType(AttributeType.UUIDType)}",
        when(root.projectUsesAuth) { "token string" },
      ),
      "(bool, error)",
    )

  private[service] def generateInterface(root: ServiceRoot): String =
    mkCode.lines(
      "// Comm provides the interface adopted by the Handler, allowing for mocking",
      genInterface(
        "Comm",
        root.comms.map { serviceName =>
          generateCommFunctionDecl(root, serviceName)
        },
      ),
    )

  private[service] def generateHandlerStruct(): String =
    mkCode.lines(
      "// Handler maintains the list of services and their associated hostnames",
      genStruct("Handler", ListMap("Services" -> "map[string]string")),
    )

  private def generateCommFunction(root: ServiceRoot, serviceName: String): String =
    mkCode.lines(
      s"// Check${serviceName.capitalize} makes a request to the target service to check if a $serviceName${root.idAttribute.name.toUpperCase} exists",
      mkCode(
        s"func (comm *Handler) ${generateCommFunctionDecl(root, serviceName)}",
        CodeWrap.curly.tabbed(
          genDeclareAndAssign(s"comm.Services[${doubleQuote(serviceName)}]", "hostname", "ok"),
          genIf(
            "!ok",
            genReturn(
              "false",
              s"errors.New(${doubleQuote(s"service $serviceName's hostname is not in the config file")})",
            ),
          ),
          "",
          genDeclareAndAssign(
            s"http.NewRequest(http.MethodGet, fmt.Sprintf(${doubleQuote("%s/%s")}, hostname, $serviceName${root.idAttribute.name.toUpperCase}.String()), nil)",
            "req",
            "err",
          ),
          genCheckAndReturnError("false"),
          "",
          when(root.projectUsesAuth) {
            mkCode.lines(
              "// Token should already be in the form `Bearer <token>`",
              s"req.Header.Set(${doubleQuote("Authorization")}, token)",
            )
          },
          genDeclareAndAssign("new(http.Client).Do(req)", "resp", "err"),
          genCheckAndReturnError("false"),
          "defer resp.Body.Close()",
          "",
          genReturn("resp.StatusCode == http.StatusOK", "nil"),
        ),
      ),
    )

  private[service] def generateCommFunctions(root: ServiceRoot): String =
    mkCode.doubleLines(
      root.comms.map { serviceName =>
        generateCommFunction(root, serviceName)
      },
    )
}
