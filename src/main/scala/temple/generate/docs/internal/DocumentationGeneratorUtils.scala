package temple.generate.docs.internal

import io.circe.Json
import temple.ast.AbstractAttribute
import temple.ast.AttributeType._
import temple.generate.CRUD
import temple.generate.CRUD.CRUD
import io.circe.syntax._

import scala.Option.when

object DocumentationGeneratorUtils {

  private[docs] def httpMethod(crud: CRUD): String = crud match {
    case CRUD.List | CRUD.Read | CRUD.Identify => "GET"
    case CRUD.Create                           => "POST"
    case CRUD.Update                           => "PUT"
    case CRUD.Delete                           => "DELETE"
  }

  private[docs] def httpCodeString(code: Int): String = code match {
    case 200  => "200 OK"
    case 302  => "302 FOUND"
    case 400  => "400 BAD REQUEST"
    case 401  => "401 UNAUTHORIZED"
    case 403  => "403 FORBIDDEN"
    case 404  => "404 NOT FOUND"
    case 500  => "500 INTERNAL SERVER ERROR"
    case code => throw new RuntimeException(s"Unhandled HTTP Code $code")
  }

  private[docs] def errorCodes(crud: CRUD, usesAuth: Boolean): Set[Int] = {
    val standard = crud match {
      case CRUD.List                             => Set(500)
      case CRUD.Create                           => Set(400, 500)
      case CRUD.Read | CRUD.Update | CRUD.Delete => Set(400, 404, 500)
      case CRUD.Identify                         => Set(404, 500)
    }
    standard ++ when(usesAuth) { 401 }
  }

  private[docs] def description(entity: String, crud: CRUD, struct: Option[String]): String = {
    val structText = struct.fold(entity)(name => s"$name belonging to the provided $entity")
    crud match {
      case CRUD.List     => s"Get a list of every $structText"
      case CRUD.Create   => s"Register a new $structText"
      case CRUD.Read     => s"Look up a single $structText"
      case CRUD.Update   => s"Update a single $structText"
      case CRUD.Delete   => s"Delete a single $structText"
      case CRUD.Identify => s"Look up the single $entity associated with the access token"
    }
  }

  private[docs] def url(service: String, crud: CRUD, structName: Option[String]): String = {
    val base = s"/api/$service${structName.fold("")(name => s"/{parent_id}/$name")}"
    crud match {
      case CRUD.List                             => s"$base/all"
      case CRUD.Create | CRUD.Identify           => base
      case CRUD.Read | CRUD.Update | CRUD.Delete => s"$base/{id}"
    }
  }

  private[docs] def generateRequestBody(attributes: Map[String, AbstractAttribute]): Seq[Seq[String]] =
    attributes.map {
      case (name, attr) =>
        val (attributeType, details) = attr.attributeType match {
          case ForeignKey(references) => ("UUID", s"Must reference an existing $references")
          case UUIDType               => ("UUID", "")
          case BoolType               => ("Boolean", "")
          case DateType               => ("Date", "Format: 'YYYY-MM-DD'")
          case DateTimeType           => ("DateTime", "Format: 'YYYY-MM-DDTHH:MM:SS.NNNNNN'")
          case TimeType               => ("Time", "Format: 'HH:MM:SS.NNNNNN'")
          case BlobType(size)         => ("Base64 String", size.fold("")(bytes => s"Max Size: $bytes bytes"))
          case StringType(max, min) =>
            val minString = min.map(v => s"Min Length: $v")
            val maxString = max.map(v => s"Max Length: $v")
            ("String", Seq(minString, maxString).flatten.mkString(", "))
          case IntType(max, min, precision) =>
            val minString       = min.map(v => s"Min Value: $v")
            val maxString       = max.map(v => s"Max Value: $v")
            val precisionString = Some(s"Precision: $precision bytes")
            ("Integer", Seq(minString, maxString, precisionString).flatten.mkString(", "))
          case FloatType(max, min, precision) =>
            val minString       = min.map(v => s"Min Value: $v")
            val maxString       = max.map(v => s"Max Value: $v")
            val precisionString = Some(s"Precision: $precision bytes")
            ("Float", Seq(minString, maxString, precisionString).flatten.mkString(", "))
        }
        Seq(name, attributeType, details)
    }.toSeq

  private[docs] def generateResponseBody(attributes: Map[String, AbstractAttribute]): Json =
    attributes.view
      .mapValues { attr =>
        attr.attributeType match {
          case ForeignKey(_) | UUIDType => "00000000-0000-0000-0000-000000000000".asJson
          case BoolType                 => true.asJson
          case DateType                 => "2020-01-01".asJson
          case DateTimeType             => "2020-01-01T23:59:59".asJson
          case TimeType                 => "23:59:59".asJson
          case BlobType(_)              => "data".asJson
          case StringType(_, _)         => "string-contents".asJson
          case IntType(_, _, _)         => 42.asJson
          case FloatType(_, _, _)       => 42.0.asJson
        }
      }
      .toMap
      .asJson
}
