package temple.generate.metrics.grafana

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.metrics.grafana.GrafanaDashboardGeneratorTestUtils.{makeTarget, _}
import temple.generate.metrics.grafana.ast.Row.{Metric, Query}
import temple.generate.metrics.grafana.ast.{Datasource, Row}
import temple.generate.utils.CodeTerm.mkCode

class GrafanaDashboardGeneratorTest extends FlatSpec with Matchers {
  behavior of "GrafanaDashboardGenerator"

  it should "generate correct JSON with no panels" in {
    val expected  = makeDashboard("abcdefg", "Example Dashboard", "")
    val generated = GrafanaDashboardGenerator.generate("abcdefg", "Example", Seq())
    generated shouldBe expected
  }

  it should "generate correct JSON with a single panel in a single row with no queries" in {
    val expected = makeDashboard(
      "abcdefg",
      "Example Dashboard",
      makePanel(1, "Prometheus", "My Example Metric", 24, 5, 0, 0, "MyYAxis"),
    )

    val generated = GrafanaDashboardGenerator
      .generate(
        "abcdefg",
        "Example",
        Seq(
          Row(
            Metric(1, "My Example Metric", Datasource.Prometheus("Prometheus", "http://prom:9090"), "MyYAxis", Seq()),
          ),
        ),
      )

    generated shouldBe expected
  }

  it should "generate correct JSON with 4 panels in a single row with no queries" in {
    val expected = makeDashboard(
      "abcdefg",
      "Example Dashboard",
      makePanel(1, "Prometheus", "My First Metric", 6, 5, 0, 0, "MyYAxis1"),
      makePanel(2, "Prometheus", "My Second Metric", 6, 5, 6, 0, "MyYAxis2"),
      makePanel(3, "Prometheus", "My Third Metric", 6, 5, 12, 0, "MyYAxis3"),
      makePanel(4, "Prometheus", "My Fourth Metric", 6, 5, 18, 0, "MyYAxis4"),
    )

    val datasource = Datasource.Prometheus("Prometheus", "http://prom:9090")
    val generated = GrafanaDashboardGenerator.generate(
      "abcdefg",
      "Example",
      Seq(
        Row(
          Metric(1, "My First Metric", datasource, "MyYAxis1", Seq()),
          Metric(2, "My Second Metric", datasource, "MyYAxis2", Seq()),
          Metric(3, "My Third Metric", datasource, "MyYAxis3", Seq()),
          Metric(4, "My Fourth Metric", datasource, "MyYAxis4", Seq()),
        ),
      ),
    )

    generated shouldBe expected
  }

  it should "generate correct JSON with 4 panels in a single row with queries" in {
    val expected = makeDashboard(
      "abcdefg",
      "Example Dashboard",
      makePanel(1, "Prometheus", "My First Metric", 6, 5, 0, 0, "MyYAxis1"),
      makePanel(2, "Prometheus", "My Second Metric", 6, 5, 6, 0, "MyYAxis2"),
      makePanel(3, "Prometheus", "My Third Metric", 6, 5, 12, 0, "MyYAxis3"),
      makePanel(4, "Prometheus", "My Fourth Metric", 6, 5, 18, 0, "MyYAxis4"),
    )

    val datasource = Datasource.Prometheus("Prometheus", "http://prom:9090")
    val generated = GrafanaDashboardGenerator.generate(
      "abcdefg",
      "Example",
      Seq(
        Row(
          Metric(1, "My First Metric", datasource, "MyYAxis1", Seq()),
          Metric(2, "My Second Metric", datasource, "MyYAxis2", Seq()),
          Metric(3, "My Third Metric", datasource, "MyYAxis3", Seq()),
          Metric(4, "My Fourth Metric", datasource, "MyYAxis4", Seq()),
        ),
      ),
    )

    generated shouldBe expected
  }

  it should "generate correct JSON with 2 panels in 2 rows with no queries" in {
    val expected = makeDashboard(
      "abcdefg",
      "Example Dashboard",
      makePanel(1, "Prometheus", "My First Metric", 12, 5, 0, 0, "MyYAxis1"),
      makePanel(2, "Prometheus", "My Second Metric", 12, 5, 12, 0, "MyYAxis2"),
      makePanel(3, "Prometheus", "My Third Metric", 12, 5, 0, 5, "MyYAxis3"),
      makePanel(4, "Prometheus", "My Fourth Metric", 12, 5, 12, 5, "MyYAxis4"),
    )

    val datasource = Datasource.Prometheus("Prometheus", "http://prom:9090")
    val generated = GrafanaDashboardGenerator.generate(
      "abcdefg",
      "Example",
      Seq(
        Row(
          Metric(1, "My First Metric", datasource, "MyYAxis1", Seq()),
          Metric(2, "My Second Metric", datasource, "MyYAxis2", Seq()),
        ),
        Row(
          Metric(3, "My Third Metric", datasource, "MyYAxis3", Seq()),
          Metric(4, "My Fourth Metric", datasource, "MyYAxis4", Seq()),
        ),
      ),
    )

    generated shouldBe expected
  }
  it should "generate correct JSON with 2 panels in 2 rows with a single query per panel" in {
    val targets =
      List("first", "second", "third", "fourth").map(name => makeTarget(s"rate(my_${name}_metric)", "Success", "A"))

    val expected = makeDashboard(
      "abcdefg",
      "Example Dashboard",
      makePanel(1, "Prometheus", "My First Metric", 12, 5, 0, 0, "MyYAxis1", targets(0)),
      makePanel(2, "Prometheus", "My Second Metric", 12, 5, 12, 0, "MyYAxis2", targets(1)),
      makePanel(3, "Prometheus", "My Third Metric", 12, 5, 0, 5, "MyYAxis3", targets(2)),
      makePanel(4, "Prometheus", "My Fourth Metric", 12, 5, 12, 5, "MyYAxis4", targets(3)),
    )

    val datasource = Datasource.Prometheus("Prometheus", "http://prom:9090")
    val topRow = Seq(
      Row(
        Metric(
          1,
          "My First Metric",
          datasource,
          "MyYAxis1",
          Seq(Query("rate(my_first_metric)", "Success")),
        ),
        Metric(
          2,
          "My Second Metric",
          datasource,
          "MyYAxis2",
          Seq(Query("rate(my_second_metric)", "Success")),
        ),
      ),
    )
    val bottomRow = Seq(
      Row(
        Metric(
          3,
          "My Third Metric",
          datasource,
          "MyYAxis3",
          Seq(Query("rate(my_third_metric)", "Success")),
        ),
        Metric(
          4,
          "My Fourth Metric",
          datasource,
          "MyYAxis4",
          Seq(Query("rate(my_fourth_metric)", "Success")),
        ),
      ),
    )

    val generated = GrafanaDashboardGenerator.generate("abcdefg", "Example", topRow ++ bottomRow)

    generated shouldBe expected
  }

  it should "generate correct JSON with 2 panels in 2 rows with multiple queries per panel" in {
    val targets =
      List("first", "second", "third", "fourth").map(name =>
        Seq(
          makeTarget(s"rate(my_${name}_metric)", "Success", "A"),
          makeTarget(s"rate(my_other_${name}_metric)", "Failure", "B"),
        ),
      )
    val expected = makeDashboard(
      "abcdefg",
      "Example Dashboard",
      makePanel(1, "Prometheus", "My First Metric", 12, 5, 0, 0, "MyYAxis1", targets(0): _*),
      makePanel(2, "Prometheus", "My Second Metric", 12, 5, 12, 0, "MyYAxis2", targets(1): _*),
      makePanel(3, "Prometheus", "My Third Metric", 12, 5, 0, 5, "MyYAxis3", targets(2): _*),
      makePanel(4, "Prometheus", "My Fourth Metric", 12, 5, 12, 5, "MyYAxis4", targets(3): _*),
    )

    val queries =
      List("first", "second", "third", "fourth").map(name =>
        Seq(
          Query(s"rate(my_${name}_metric)", "Success"),
          Query(s"rate(my_other_${name}_metric)", "Failure"),
        ),
      )

    val datasource = Datasource.Prometheus("Prometheus", "http://prom:9090")
    val topRow = Seq(
      Row(
        Metric(1, "My First Metric", datasource, "MyYAxis1", queries(0)),
        Metric(2, "My Second Metric", datasource, "MyYAxis2", queries(1)),
      ),
    )
    val bottomRow = Seq(
      Row(
        Metric(3, "My Third Metric", datasource, "MyYAxis3", queries(2)),
        Metric(4, "My Fourth Metric", datasource, "MyYAxis4", queries(3)),
      ),
    )
    val generated = GrafanaDashboardGenerator.generate("abcdefg", "Example", topRow ++ bottomRow)

    generated shouldBe expected
  }
}
