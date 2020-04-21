package temple.generate.server.go.service.dao

import temple.ast.Metadata.Readable
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.server.{AttributesRoot, ServiceName}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when

object GoServiceDAOInterfaceGenerator {

  private def generateInterfaceFunctionReturnType(block: ServiceName, operation: CRUD): String =
    operation match {
      case List                              => s"(*[]${block.name}, error)"
      case Create | Read | Update | Identify => s"(*${block.name}, error)"
      case Delete                            => "error"
    }

  private[dao] def generateInterfaceFunction(block: AttributesRoot, operation: CRUD): String = {
    val functionName = generateDAOFunctionName(block, operation)
    val functionArgs =
      if (block.readable == Readable.This || operation != List) s"input ${functionName}Input" else ""
    mkCode(
      CodeWrap.parens
        .prefix(functionName)
        .list(
          when(block.readable == Readable.This || operation != List || block.parentAttribute.isDefined) {
            s"input ${functionName}Input"
          },
        ),
      generateInterfaceFunctionReturnType(block, operation),
    )
  }

  private[service] def generateInterface(root: ServiceRoot): String =
    mkCode.lines(
      "// BaseDatastore provides the basic datastore methods",
      mkCode(
        "type BaseDatastore interface",
        CodeWrap.curly.tabbed(
          mkCode.doubleLines(
            root.blockIterator.map { block =>
              mkCode.lines(
                block.operations.toSeq.map { operation =>
                  generateInterfaceFunction(block, operation)
                },
              )
            },
          ),
        ),
      ),
    )
}
