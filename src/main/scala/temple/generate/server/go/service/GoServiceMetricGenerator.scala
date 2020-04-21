package temple.generate.server.go.service

import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonMetricGenerator
import temple.generate.utils.CodeUtils
import temple.utils.StringUtils.doubleQuote

object GoServiceMetricGenerator {

  // Generate global variables for metrics, including string identifiers and metric objects
  private[service] def generateVars(root: ServiceRoot): String = {
    // Assign strings to variables of form `RequestCreate = "create"`
    val serviceGlobals = CodeUtils.pad(
      root.blockIterator.flatMap { block =>
        block.operations.toSeq.map { operation =>
          (
            s"Request${operation.toString.capitalize}${block.name}",
            doubleQuote(s"${operation.toString.toLowerCase}_${block.snakeName}"),
          )
        }
      }.toSeq,
      separator = " = ",
    )

    GoCommonMetricGenerator.generateVars(serviceGlobals, root.name)
  }
}
