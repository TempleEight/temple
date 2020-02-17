package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}

class TempleBlockTest extends FlatSpec with Matchers {

  behavior of "TempleBlock"

  it should "lookupLocalMetadata for project block" in {
    val projectBlock = ProjectBlock(Seq(Metadata.Database.Postgres))
    projectBlock.lookupLocalMetadata[Metadata.Language] shouldBe None
    projectBlock.lookupLocalMetadata[Metadata.Database] shouldBe Some(Metadata.Database.Postgres)

    val targetBlock = TargetBlock(Seq(Metadata.Language.JavaScript))
    targetBlock.lookupLocalMetadata[Metadata.Language] shouldBe Some(Metadata.Language.JavaScript)
    targetBlock.lookupLocalMetadata[Metadata.Database] shouldBe None
  }

  it should "fail to lookupMetadata without being in a project file" in {
    val projectBlock = ProjectBlock(Seq(Metadata.Language.JavaScript))
    a[NullPointerException] should be thrownBy { projectBlock.lookupMetadata[Metadata.Database] }
  }

  it should "lookupMetadata when in a project file" in {
    val projectBlock = ProjectBlock(Seq(Metadata.Language.JavaScript, Metadata.Database.Postgres))
    val serviceBlock = ServiceBlock(Map.empty, Seq(Metadata.Language.Go))

    Templefile("TestProject", projectBlock, Map.empty, Map("Users" -> serviceBlock))

    serviceBlock.lookupMetadata[Metadata.Language] shouldBe Some(Metadata.Language.Go)
    serviceBlock.lookupMetadata[Metadata.Database] shouldBe Some(Metadata.Database.Postgres)
    serviceBlock.lookupMetadata[Metadata.Uses] shouldBe None
  }
}
