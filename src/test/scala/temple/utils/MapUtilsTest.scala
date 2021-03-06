package temple.utils

import org.scalatest.{FlatSpec, Matchers}
import temple.errors.FailureContext
import temple.utils.MapUtils.SafeInsertMap

import scala.collection.mutable

class MapUtilsTest extends FlatSpec with Matchers {

  behavior of "SafeInsertMap"

  it should "allow any insertion into an empty map" in {
    val map: mutable.Map[Int, Boolean] = mutable.HashMap()

    map.safeInsert(1 -> true, fail("Conflict incorrectly found in map"))
  }

  it should "allow non-colliding insertions into a map" in {
    val map: mutable.Map[Int, Boolean] = mutable.HashMap(1 -> true, 2 -> false)

    map.safeInsert(3 -> true, fail("Conflict incorrectly found in map"))
    map.safeInsert(0 -> false, fail("Conflict incorrectly found in map"))
  }

  it should "fail on duplicate insertions into a map" in {
    var callbackCalled = 0

    val map: mutable.Map[Int, Boolean] = mutable.HashMap()

    map.safeInsert(3 -> true, fail("Conflict incorrectly found in map"))
    map.safeInsert(3 -> false, callbackCalled += 1)

    callbackCalled shouldBe 1
  }

  it should "fail on conflicting insertions into a map" in {
    var callbackCalled = 0

    val map: mutable.Map[Int, Boolean] = mutable.HashMap(3 -> true)

    map.safeInsert(3 -> false, callbackCalled += 1)

    callbackCalled shouldBe 1
  }

  it should "fail using an implicit fail handler" in {
    var callbackCalled = 0

    val map: mutable.Map[Int, Boolean] = mutable.HashMap(3 -> true)

    implicit val failureContext: FailureContext = _ => callbackCalled += 1

    map.safeInsert(1 -> true)

    callbackCalled shouldBe 0

    map.safeInsert(3 -> false)

    callbackCalled shouldBe 1
  }

}
