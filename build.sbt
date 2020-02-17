name := "temple"

version := "0.1"

scalaVersion := "2.13.1"

// https://www.scala-sbt.org/1.x/docs/Testing.html#Integration+Tests
lazy val EndToEndTest = config("e2e") extend(Test)
lazy val endToEndTestSettings: Seq[Def.Setting[_]] =
  inConfig(EndToEndTest)(Defaults.testSettings) ++
    Seq(
      scalaSource in EndToEndTest := baseDirectory.value / "src/e2e/scala",
      parallelExecution in EndToEndTest := false,
      fork in EndToEndTest := true
    )

lazy val root = (project in file("."))
  .configs(IntegrationTest, EndToEndTest)
  .settings(endToEndTestSettings)
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
      "com.beachape" %% "enumeratum" % "1.5.15",
      "io.circe" %% "circe-core" % "0.12.3",
      "io.circe" %% "circe-generic" % "0.12.3",
      "io.circe" %% "circe-parser" % "0.12.3"
    )
  )

scalacOptions ++= Seq("-deprecation", "-feature")

// https://github.com/scoverage/sbt-scoverage#exclude-classes-and-packages
coverageExcludedPackages := "<empty>;temple\\.Main;"

// Enable formatting on integration tests
inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)
