package temple.DSL.semantics

import temple.utils.StringUtils

import scala.collection.Iterator.iterate

object NameClashes {

  // https://www.postgresql.org/docs/10/sql-keywords-appendix.html
  // only those marked as "reserved"/"reserved (can be function or type)" in Postgres
  private val postgresNames = Set(
    "all",
    "analyse",
    "analyze",
    "and",
    "any",
    "array",
    "as",
    "asc",
    "asymmetric",
    "authorization",
    "binary",
    "both",
    "case",
    "cast",
    "check",
    "collate",
    "collation",
    "column",
    "concurrently",
    "constraint",
    "create",
    "cross",
    "current_catalog",
    "current_date",
    "current_role",
    "current_schema",
    "current_time",
    "current_timestamp",
    "current_user",
    "default",
    "deferrable",
    "desc",
    "distinct",
    "do",
    "else",
    "end",
    "except",
    "false",
    "fetch",
    "for",
    "foreign",
    "freeze",
    "from",
    "full",
    "grant",
    "group",
    "having",
    "ilike", // sql, it’s my favourite language
    "in",
    "initially",
    "inner",
    "intersect",
    "into",
    "is",
    "isnull",
    "join",
    "lateral",
    "leading",
    "left",
    "like",
    "limit",
    "localtime",
    "localtimestamp",
    "natural",
    "not",
    "notnull",
    "null",
    "offset",
    "on",
    "only",
    "or",
    "order",
    "outer",
    "overlaps",
    "placing",
    "primary",
    "references",
    "returning",
    "right",
    "select",
    "session_user",
    "similar",
    "some",
    "symmetric",
    "table",
    "tablesample",
    "then",
    "to",
    "trailing",
    "true",
    "union",
    "unique",
    "user",
    "using",
    "variadic",
    "verbose",
    "when",
    "where",
    "window",
    "with",
  )

  case class NameValidator private (private[NameClashes] val isValid: String => Boolean)

  private[NameClashes] object NameValidator {
    private val acceptEverything = NameValidator(_ => true)

    def combine(validators: IterableOnce[NameValidator]): NameValidator =
      validators.iterator.foldLeft(acceptEverything) { (v1, v2) =>
        NameValidator(str => v1.isValid(str) && v2.isValid(str))
      }

    def fromIterable(strings: Set[String]): NameValidator = NameValidator { name =>
      !strings.iterator.contains(name.toLowerCase)
    }
  }

  val postgresValidator: NameValidator = NameValidator.fromIterable(postgresNames)

  /** Repeatedly add the project name to the start of the string */
  def nameSuggestions(name: String, project: String, decapitalize: Boolean = false): Iterator[String] =
    iterate(name.capitalize) { project + _ }.map { if (decapitalize) StringUtils.decapitalize else identity }

  def dodgeNames(name: String, project: String, takenNames: Set[String], decapitalize: Boolean = false)(
    validators: NameValidator*,
  ): String = {
    val validator = NameValidator.combine(Iterator(NameValidator.fromIterable(takenNames)) ++ validators)
    nameSuggestions(name, project, decapitalize).filter { validator.isValid(_) }.next
  }
}
