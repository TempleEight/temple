package temple.generate.server.go.common

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

object GoCommonDAOGenerator {

  private[go] def generateDAOStruct(): String =
    mkCode.lines(
      "// DAO encapsulates access to the datastore",
      mkCode("type DAO struct", CodeWrap.curly.tabbed("DB *sql.DB")),
    )

  private[go] def generateInit(): String =
    mkCode.lines(
      "// Init opens the datastore connection, returning a DAO",
      mkCode(
        "func Init(config *util.Config) (*DAO, error)",
        CodeWrap.curly.tabbed(
          s"connStr := fmt.Sprintf(${doubleQuote("user=%s dbname=%s host=%s sslmode=%s")}, config.User, config.DBName, config.Host, config.SSLMode)",
          s"db, err := sql.Open(${doubleQuote("postgres")}, connStr)",
          mkCode(
            "if err != nil",
            CodeWrap.curly.tabbed(
              "return nil, err",
            ),
          ),
          "return &DAO{db}, nil",
        ),
      ),
    )

  private[go] def generateExecuteQuery(): String =
    mkCode.lines(
      "// Executes a query, returning the number of rows affected",
      mkCode(
        "func executeQuery(db *sql.DB, query string, args ...interface{}) (int64, error)",
        CodeWrap.curly.tabbed(
          "result, err := db.Exec(query, args...)",
          mkCode(
            "if err != nil",
            CodeWrap.curly.tabbed(
              "return 0, err",
            ),
          ),
          "return result.RowsAffected()",
        ),
      ),
    )

  private[go] def generateExecuteQueryWithRowResponse(): String =
    mkCode.lines(
      "// Executes a query, returning the row",
      mkCode(
        "func executeQueryWithRowResponse(db *sql.DB, query string, args ...interface{}) *sql.Row",
        CodeWrap.curly.tabbed(
          "return db.QueryRow(query, args...)",
        ),
      ),
    )

  private[go] def generateExecuteQueryWithRowResponses(): String =
    mkCode.lines(
      "// Executes a query, returning the rows",
      mkCode(
        "func executeQueryWithRowResponses(db *sql.DB, query string, args ...interface{}) (*sql.Rows, error)",
        CodeWrap.curly.tabbed(
          "return db.Query(query, args...)",
        ),
      ),
    )
}
