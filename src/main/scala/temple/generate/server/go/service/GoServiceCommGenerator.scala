package temple.generate.server.go.service

import temple.ast.AttributeType
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.ServiceName
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

  private[service] def generateCommFunctionDecl(root: ServiceRoot, serviceName: ServiceName): String =
    mkCode(
      genFunctionCall(
        s"Check${serviceName.name}",
        s"${serviceName.decapitalizedName}${root.idAttribute.name.toUpperCase} ${generateGoType(AttributeType.UUIDType)}",
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

  private def generateCommFunction(root: ServiceRoot, serviceName: ServiceName): String =
    mkCode.lines(
      s"// Check${serviceName.name} makes a request to the target service to check if a ${serviceName.decapitalizedName}${root.idAttribute.name.toUpperCase} exists",
      mkCode(
        s"func (comm *Handler) ${generateCommFunctionDecl(root, serviceName)}",
        CodeWrap.curly.tabbed(
          genDeclareAndAssign(s"comm.Services[${doubleQuote(serviceName.kebabName)}]", "hostname", "ok"),
          genIf(
            "!ok",
            genReturn(
              "false",
              s"errors.New(${doubleQuote(s"service ${serviceName.decapitalizedName}'s hostname is not in the config file")})",
            ),
          ),
          "",
          genDeclareAndAssign(
            s"http.NewRequest(http.MethodGet, fmt.Sprintf(${doubleQuote("%s/%s")}, hostname, ${serviceName.decapitalizedName}${root.idAttribute.name.toUpperCase}.String()), nil)",
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
