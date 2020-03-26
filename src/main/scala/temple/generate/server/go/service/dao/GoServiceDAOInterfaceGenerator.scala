package temple.generate.server.go.service.dao

import temple.generate.CRUD
import temple.generate.CRUD.{CRUD, Create, Delete, List, Read, Update}
import temple.generate.server.CreatedByAttribute
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

object GoServiceDAOInterfaceGenerator {

  private def generateInterfaceFunctionReturnType(serviceName: String, operation: CRUD): String =
    operation match {
      case List                   => s"(*[]${serviceName.capitalize}, error)"
      case Create | Read | Update => s"(*${serviceName.capitalize}, error)"
      case Delete                 => "error"
    }

  private[dao] def generateInterfaceFunction(
    serviceName: String,
    operation: CRUD,
    createdByAttribute: CreatedByAttribute,
  ): String = {
    val functionName = generateDAOFunctionName(operation, serviceName)
    val enumeratingByCreator = createdByAttribute match {
      case _: CreatedByAttribute.EnumerateByCreator => true
      case _                                        => false
    }
    val functionArgs = if (enumeratingByCreator || operation != CRUD.List) s"input ${functionName}Input" else ""
    mkCode(
      s"$functionName($functionArgs)",
      generateInterfaceFunctionReturnType(serviceName, operation),
    )
  }

  private[service] def generateInterface(
    serviceName: String,
    operations: Set[CRUD],
    createdByAttribute: CreatedByAttribute,
  ): String =
    mkCode.lines(
      "// Datastore provides the interface adopted by the DAO, allowing for mocking",
      mkCode(
        "type Datastore interface",
        CodeWrap.curly.tabbed(
          for (operation <- operations.toSeq.sorted)
            yield generateInterfaceFunction(serviceName, operation, createdByAttribute),
        ),
      ),
    )
}
