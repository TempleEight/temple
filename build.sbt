name := "temple"

version := "0.1"

scalaVersion := "2.13.1"

mainClass in assembly := Some("temple.Main")
assemblyJarName in assembly := "temple-latest.jar"

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
      "com.beachape" %% "enumeratum" % "1.5.15",
      "io.circe" %% "circe-core" % "0.12.3",
      "io.circe" %% "circe-generic" % "0.12.3",
      "io.circe" %% "circe-parser" % "0.12.3",
    )
  )

scalacOptions ++= Seq("-deprecation", "-feature")

// https://github.com/scoverage/sbt-scoverage#exclude-classes-and-packages
coverageExcludedPackages := "<empty>;temple\\.Main;"

// Enable formatting on integration tests
inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)

//https://stackoverflow.com/questions/28459333/how-to-build-an-uber-jar-fat-jar-using-sbt-within-intellij-idea
// META-INF discarding
assemblyMergeStrategy in assembly ~= { _ =>
  {
    case PathList("META-INF", _*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
}
