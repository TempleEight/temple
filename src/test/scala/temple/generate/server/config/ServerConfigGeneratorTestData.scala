package temple.generate.server.config

object ServerConfigGeneratorTestData {

  val serverConfig: String =
    """{
      |  "user" : "postgres",
      |  "dbName" : "postgres",
      |  "host" : "match-db",
      |  "sslMode" : "disable",
      |  "services" : {
      |    "user" : "http://user:80/user"
      |  },
      |  "ports" : {
      |    "service" : 81
      |  }
      |}
      |""".stripMargin

  val serverConfigWithMetrics: String =
    """{
      |  "user" : "postgres",
      |  "dbName" : "postgres",
      |  "host" : "match-db",
      |  "sslMode" : "disable",
      |  "services" : {
      |    "user" : "http://user:80/user"
      |  },
      |  "ports" : {
      |    "service" : 81,
      |    "prometheus" : 2113
      |  }
      |}
      |""".stripMargin
}
