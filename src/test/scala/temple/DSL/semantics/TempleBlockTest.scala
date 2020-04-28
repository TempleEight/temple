package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.AbstractServiceBlock._
import temple.ast._

class TempleBlockTest extends FlatSpec with Matchers {

  behavior of "TempleBlock"

  it should "lookupLocalMetadata for project block" in {
    val projectBlock = ProjectBlock(Seq(Metadata.Database.Postgres))
    projectBlock.lookupLocalMetadata[Metadata.ServiceLanguage] shouldBe None
    projectBlock.lookupLocalMetadata[Metadata.Database] shouldBe Some(Metadata.Database.Postgres)
  }

  it should "fail to lookupMetadata without being in a project file" in {
    val projectBlock = ProjectBlock(Seq(Metadata.ServiceLanguage.Go))
    a[NullPointerException] should be thrownBy { projectBlock.lookupMetadata[Metadata.Database] }
  }

  it should "lookupMetadata when in a project file" in {
    val projectBlock = ProjectBlock(Seq(Metadata.Database.Postgres))
    val serviceBlock = ServiceBlock(Map.empty, Seq(Metadata.ServiceLanguage.Go))

    Templefile("TestProject", projectBlock, services = Map("Users" -> serviceBlock))

    serviceBlock.lookupMetadata[Metadata.ServiceLanguage] shouldBe Some(Metadata.ServiceLanguage.Go)
    serviceBlock.lookupMetadata[Metadata.Database] shouldBe Some(Metadata.Database.Postgres)
    serviceBlock.lookupMetadata[Metadata.Provider] shouldBe None
  }

  it should "lookupMetadata in a struct block in a project file" in {
    val projectBlock = ProjectBlock(Seq(Metadata.Database.Postgres))
    val structBlock  = StructBlock(Map.empty)
    val serviceBlock = ServiceBlock(Map.empty, Seq(Metadata.ServiceLanguage.Go), Map("Struct" -> structBlock))

    Templefile("TestProject", projectBlock, services = Map("Users" -> serviceBlock))

    structBlock.lookupMetadata[Metadata.ServiceLanguage] shouldBe Some(Metadata.ServiceLanguage.Go)
    structBlock.lookupMetadata[Metadata.Database] shouldBe Some(Metadata.Database.Postgres)
    structBlock.lookupMetadata[Metadata.Provider] shouldBe None
  }
}
