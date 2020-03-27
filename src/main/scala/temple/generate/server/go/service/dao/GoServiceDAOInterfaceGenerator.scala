package temple.generate.server.go.service.dao

import temple.generate.CRUD
import temple.generate.CRUD.{CRUD, Create, Delete, List, Read, Update}
import temple.generate.server.{CreatedByAttribute, ServiceRoot}
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

object GoServiceDAOInterfaceGenerator {

  private def generateInterfaceFunctionReturnType(root: ServiceRoot, operation: CRUD): String =
    operation match {
      case List                   => s"(*[]${root.name.capitalize}, error)"
      case Create | Read | Update => s"(*${root.name.capitalize}, error)"
      case Delete                 => "error"
    }

  private[dao] def generateInterfaceFunction(root: ServiceRoot, operation: CRUD): String = {
    val functionName = generateDAOFunctionName(root, operation)
    val enumeratingByCreator = root.createdByAttribute match {
      case _: CreatedByAttribute.EnumerateByCreator => true
      case _                                        => false
    }
    val functionArgs = if (enumeratingByCreator || operation != CRUD.List) s"input ${functionName}Input" else ""
    mkCode(
      s"$functionName($functionArgs)",
      generateInterfaceFunctionReturnType(root, operation),
    )
  }

  private[service] def generateInterface(root: ServiceRoot, operations: Set[CRUD]): String =
    mkCode.lines(
      "// Datastore provides the interface adopted by the DAO, allowing for mocking",
      mkCode(
        "type Datastore interface",
        CodeWrap.curly.tabbed(
          for (operation <- operations.toSeq.sorted)
            yield generateInterfaceFunction(root, operation),
        ),
      ),
    )
}
