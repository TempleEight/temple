package temple.generate.database

/** Generation context for Postgres databases, particularly stores which type of prepared statements to use */
case class PostgresContext(preparedType: PreparedType) extends DatabaseContext
