package temple.DSL.semantics

import temple.generate.CRUD
import temple.utils.StringUtils

import scala.collection.Iterator.iterate

object NameClashes {

  val templeAttributeNameValidator: NameValidator = NameValidator.fromBlacklist(
    "id",
    "createdBy",
  )

  val templeServiceNameValidator: NameValidator = NameValidator.fromBlacklist(
    "auth",
    "api",
    "kong",
    "grafana",
    "kube",
    "prometheus",
  )

  private val goGlobalsValidator: NameValidator = NameValidator.fromBlacklist(
    //constants
    "true",
    "false",
    "iota",
    // variables
    "nil",
    // functions
    "append",
    "cap",
    "close",
    "complex",
    "copy",
    "delete",
    "imag",
    "len",
    "make",
    "new",
    "panic",
    "print",
    "println",
    "real",
    "recover",
  )

  // https://golang.org/ref/spec#Keywords
  private val goKeywordValidator = NameValidator.fromBlacklist(
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
  private def templeGoNameValidator: NameValidator = {
    val crudStrings   = CRUD.values.toSeq.map(_.toString.toLowerCase)
    val suffixStrings = Seq("list", "input", "response")

    var lastBaseName: Option[String] = None
    NameValidator.fromFunction { name =>
      val baseName = lastBaseName getOrElse name
      val valid    = !startsWithAny(name, crudStrings) && !endsWithAny(name, suffixStrings)
      if (!valid && lastBaseName.nonEmpty)
        throw new SemanticParsingException(
          s"""Cannot generate good name for "$baseName", names of projects and blocks cannot """
          + s"start with [${crudStrings.mkString(", ")}], "
          + s"and names of projects, blocks and fields cannot end with [${suffixStrings.mkString(", ")}]",
        )
      lastBaseName = Some(baseName)
      valid
    }
  }

  private val templeGoServiceValidator = NameValidator.fromBlacklist(
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
    "http",
    "time",
    "dao",
    "util",
    "valid",
    "mux",
  )

  def goServiceValidator: NameValidator =
    templeGoNameValidator & goGlobalsValidator & goKeywordValidator & templeServiceNameValidator & templeGoServiceValidator

  val goAttributeValidator: NameValidator = goKeywordValidator & templeAttributeNameValidator

  // https://www.postgresql.org/docs/10/sql-keywords-appendix.html
  // only those marked as "reserved"/"reserved (can be function or type)" in Postgres
  val postgresValidator: NameValidator = NameValidator.fromBlacklist(
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

  case class NameValidator private (private val validators: Iterable[String => Boolean]) {

    def isValid(word: String): Boolean = validators.forall { validator =>
      val normalized = NameValidator.normalize(word)
      validator(normalized)
    }

    // Combine two validators
    def &(that: NameValidator): NameValidator = NameValidator(this.validators ++ that.validators)
  }

  private[NameClashes] object NameValidator {
    private def normalize(string: String): String = string.toLowerCase.replaceAll("[^a-z0-9]", "")

    // Combine many validators
    def combine(validators: IterableOnce[NameValidator]): NameValidator =
      NameValidator(validators.iterator.flatMap(_.validators).to(Seq))

    def fromBlacklistSet(strings: Set[String]): NameValidator = {
      val strippedStrings = strings.map(normalize)
      NameValidator(Seq(!strippedStrings.contains(_)))
    }

    def fromBlacklist(strings: String*): NameValidator = fromBlacklistSet(strings.toSet)

    def fromFunction(validator: String => Boolean): NameValidator = NameValidator(Seq(validator))
  }

  /** Repeatedly add the project name to the start of the string */
  private def nameSuggestions(name: String, project: String, decapitalize: Boolean = false): Iterator[String] = {
    val iterator = iterate(name.capitalize)(project + _)
    if (decapitalize) iterator.map(StringUtils.decapitalize)
    else iterator
  }

  /** Given a service/struct/field name, prepend the project name until it becomes unique and free of conflicts with
    * the host languages */
  def constructUniqueName(name: String, project: String, takenNames: Set[String])(
    validators: NameValidator*,
  ): String = {
    // if the name starts with a lower-case letter, transform the output
    val decapitalize = name.head.isLower

    // A validator for checking a name isn’t taken locally by their own code
    val takenNamesValidator = NameValidator.fromBlacklistSet(takenNames)

    // Combine the validators they provided with the validator of taken names
    val combinedValidator = NameValidator.combine(Iterator(takenNamesValidator) ++ validators)

    // Find the first valid name in the iterator of possible names
    nameSuggestions(name, project, decapitalize).find(combinedValidator.isValid).get
  }
}
