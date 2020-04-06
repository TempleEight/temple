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
    "id",
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
  private def nameSuggestions(name: String, project: String, decapitalize: Boolean = false): Iterator[String] = {
    val iterator = iterate(name.capitalize)(project + _)
    if (decapitalize) iterator.map(StringUtils.decapitalize)
    else iterator
  }

  /** Given a service/struct/field name, prepend the project name until it becomes unique and free of conflicts with
    * the host languages */
  def constructUniqueName(name: String, project: String, takenNames: Set[String], decapitalize: Boolean = false)(
    validators: NameValidator*,
  ): String = {
    // A validator for checking a name isn’t taken locally by their own code
    val takenNamesValidator = NameValidator.fromIterable(takenNames)

    // Combine the validators they provided with the validator of taken names
    val combinedValidator = NameValidator.combine(Iterator(takenNamesValidator) ++ validators)

    // Find the first valid name in the iterator of possible names
    nameSuggestions(name, project, decapitalize).find(combinedValidator.isValid).get
  }
}
