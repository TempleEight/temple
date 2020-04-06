package temple.DSL.semantics

import temple.generate.CRUD
import temple.utils.StringUtils

import scala.collection.Iterator.iterate

object NameClashes {

  val templeAttributeNameValidator: NameValidator = NameValidator.from(
    "id",
    "createdBy",
  )

  val templeServiceNameValidator: NameValidator = NameValidator.from(
    "auth",
    "api",
    "kong",
    "grafana",
    "kube",
    "prometheus",
  )

  // https://golang.org/ref/spec#Keywords
  private val goKeywordValidator = NameValidator.from(
    "break",
    "default",
    "func",
    "interface",
    "select",
    "case",
    "defer",
    "go",
    "map",
    "struct",
    "chan",
    "else",
    "goto",
    "package",
    "switch",
    "const",
    "fallthrough",
    "if",
    "range",
    "type",
    "continue",
    "for",
    "import",
    "return",
    "var",
  )

  private def startsWithAny(string: String, prefixes: IterableOnce[String]): Boolean =
    prefixes.iterator.exists(string.startsWith)

  private def endsWithAny(string: String, suffixes: IterableOnce[String]): Boolean =
    suffixes.iterator.exists(string.endsWith)

  // DAO clashes
  private val goNameValidator = NameValidator.fromFunction(name =>
    startsWithAny(name, CRUD.values.map(_.toString.toLowerCase)) || endsWithAny(name, Seq("List", "Input", "Response")),
  )

  val goServiceValidator: NameValidator = goKeywordValidator & templeServiceNameValidator &
    NameValidator.from(
      // <struct-name>.go clashes
      "auth",
      "err",
      "req",
      "uuid",
      "errMsg",
      "json",
      "flag",
      "fmt",
      "log",
      "net/http",
      "time",
      "dao",
      "util",
      "valid",
      "mux",
    ) & goNameValidator

  val goAttributeValidator: NameValidator = goKeywordValidator & templeAttributeNameValidator

  // https://www.postgresql.org/docs/10/sql-keywords-appendix.html
  // only those marked as "reserved"/"reserved (can be function or type)" in Postgres
  val postgresValidator: NameValidator = NameValidator.from(
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

  case class NameValidator private (private val validators: Set[String => Boolean]) {

    def isValid(word: String): Boolean = validators.exists(validator => validator(NameValidator.normalize(word)))

    // Combine two validators
    def &(that: NameValidator): NameValidator = NameValidator(this.validators ++ that.validators)
  }

  private[NameClashes] object NameValidator {
    private def normalize(string: String): String = string.toLowerCase.replaceAll("[^a-z0-9]", "")

    // Combine many validators
    def combine(validators: IterableOnce[NameValidator]): NameValidator =
      NameValidator(validators.iterator.flatMap(_.validators).toSet)

    def fromSet(strings: Set[String]): NameValidator = {
      val strippedStrings = strings.map(normalize)
      NameValidator(Set(strippedStrings.contains))
    }

    def from(strings: String*): NameValidator = fromSet(strings.toSet)

    def fromFunction(validator: String => Boolean): NameValidator = NameValidator(Set(validator))
  }

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
    val takenNamesValidator = NameValidator.fromSet(takenNames)

    // Combine the validators they provided with the validator of taken names
    val combinedValidator = NameValidator.combine(Iterator(takenNamesValidator) ++ validators)

    // Find the first valid name in the iterator of possible names
    nameSuggestions(name, project, decapitalize).find(combinedValidator.isValid).get
  }
}
