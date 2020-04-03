package temple.generate.server.go.service

import temple.generate.CRUD.CRUD
import temple.generate.server.ServiceRoot
import temple.generate.server.go.GoCommonMetricGenerator
import temple.utils.StringUtils.doubleQuote
import temple.generate.utils.CodeUtils

object GoServiceMetricGenerator {

  // Generate global variables for metrics, including string identifiers and metric objects
  private[service] def generateVars(root: ServiceRoot, operations: Set[CRUD]): String = {
    // Assign strings to variables of form `RequestCreate = "create"`
    val serviceGlobals = CodeUtils.pad(operations.toSeq.sorted.map { operation =>
      (s"Request${operation.toString.capitalize}", doubleQuote(operation.toString.toLowerCase))
    }, separator = " = ")

    GoCommonMetricGenerator.generateVars(serviceGlobals, root.name)
  }
}
