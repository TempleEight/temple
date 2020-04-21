package temple.generate.server.go.service

import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonMetricGenerator
import temple.generate.utils.CodeUtils
import temple.utils.StringUtils.doubleQuote

import scala.Option.when

object GoServiceMetricGenerator {

  // Generate global variables for metrics, including string identifiers and metric objects
  private[service] def generateVars(root: ServiceRoot): String = {
    // Assign strings to variables of form `RequestCreate = "create"`
    val serviceGlobals = CodeUtils.pad(
      root.blockIterator.flatMap { block =>
        block.operations.toSeq.map { operation =>
          val suffix = when(block.parentAttribute.nonEmpty) { "_" + block.snakeName }
          (
            s"Request${operation.toString.capitalize}${block.structName}",
            doubleQuote(operation.toString.toLowerCase + suffix),
          )
        }
      }.toSeq,
      separator = " = ",
    )

    GoCommonMetricGenerator.generateVars(serviceGlobals, root.name)
  }
}
