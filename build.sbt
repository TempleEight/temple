name := "temple"

version := "0.1"

scalaVersion := "2.13.1"

// https://www.scala-sbt.org/1.x/docs/Testing.html#Integration+Tests
lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
      "org.scalatest" %% "scalatest" % "3.0.8" % "it,test",
      "org.rogach" %% "scallop" % "3.3.2",
      "org.postgresql" % "postgresql" % "42.2.9",
      "com.sun.activation" % "javax.activation" % "1.2.0",
      "com.spotify" % "docker-client" % "8.9.1",
      "com.whisk" %% "docker-testkit-scalatest" % "0.9.9" % "it,test",
      "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.9" % "it,test",
    )
  )

scalacOptions ++= Seq("-deprecation", "-feature")
