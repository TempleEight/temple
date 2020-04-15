package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}

class NameClashesTest extends FlatSpec with Matchers {

  behavior of "NameClashes.constructUniqueName"

  it should "return the same result if there are no conflicts" in {
    NameClashes.constructUniqueName("box", "Tinder", takenNames = Set("username", "field"))(
      NameClashes.goAttributeValidator,
      NameClashes.postgresValidator,
    ) shouldBe "box"
  }

  it should "return the same result if there are no conflicts in the given language" in {
    NameClashes.constructUniqueName("Column", "Tinder", takenNames = Set("username", "field"))(
      NameClashes.goServiceValidator,
    ) shouldBe "Column"
  }

  it should "prepend the project name if there are conflicts in the given language" in {
    NameClashes.constructUniqueName("Column", "Tinder", takenNames = Set("Field"))(
      NameClashes.postgresValidator,
    ) shouldBe "TinderColumn"
  }

  it should "maintain leading character case in the renaming" in {
    NameClashes.constructUniqueName("column", "Tinder", takenNames = Set("field"))(
      NameClashes.postgresValidator,
    ) shouldBe "tinderColumn"
  }

  it should "not rename into an existing name" in {
    NameClashes.constructUniqueName("column", "Tinder", takenNames = Set("tinderColumn"))(
      NameClashes.postgresValidator,
    ) shouldBe "tinderTinderColumn"
  }

  it should "rename in the case of a duplicate name in scope" in {
    NameClashes.constructUniqueName("box", "Tinder", takenNames = Set("box"))(
      NameClashes.postgresValidator,
    ) shouldBe "tinderBox"
  }

  it should "check for invalid prefixes in a name" in {
    NameClashes.constructUniqueName("listBox", "Tinder", takenNames = Set("unimportant"))(
      NameClashes.goServiceValidator,
    ) shouldBe "tinderListBox"
  }

  it should "throw if no amount of renaming can fix something" in {
    NameClashes.constructUniqueName("Fred", "CreateMachine", takenNames = Set("unimportant"))(
      NameClashes.goServiceValidator,
    ) shouldBe "Fred"

    the[SemanticParsingException] thrownBy {
      NameClashes.constructUniqueName("Func", "CreateMachine", takenNames = Set("unimportant"))(
        NameClashes.goServiceValidator,
      )
    } should have message """Cannot generate good name for "func", names of projects and blocks cannot start with [list, create, read, update, delete, identify], and names of projects, blocks and fields cannot end with [list, input, response]"""

    the[SemanticParsingException] thrownBy {
      NameClashes.constructUniqueName("List", "Tinder", takenNames = Set("unimportant"))(
        NameClashes.goServiceValidator,
      )
    } should have message """Cannot generate good name for "list", names of projects and blocks cannot start with [list, create, read, update, delete, identify], and names of projects, blocks and fields cannot end with [list, input, response]"""
  }
}
