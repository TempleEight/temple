package temple.generate.server.go.common

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.server.go.common.GoCommonGenerator._
import temple.utils.StringUtils.doubleQuote

object GoCommonMainGenerator {

  private[go] def generateJsonMiddleware(): String =
    mkCode(
      "func jsonMiddleware(next http.Handler) http.Handler",
      CodeWrap.curly.tabbed(
        genReturn(
          s"http.${genFunctionCall(
            "HandlerFunc",
            mkCode(
              genFunctionCall("func", "w http.ResponseWriter", "r *http.Request"),
              CodeWrap.curly.tabbed(
                "// All responses are JSON, set header accordingly",
                s"w.Header().Set(${doubleQuote("Content-Type")}, ${doubleQuote("application/json")})",
                "next.ServeHTTP(w, r)",
              ),
            ),
          )}",
        ),
      ),
    )
}
