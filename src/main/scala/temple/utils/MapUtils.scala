package temple.utils

import scala.collection.mutable

object MapUtils {
  type FailThrower = String => Nothing
  type FailHandler = String => Unit

  /**
    * Add `safeInsert` function to a map, which throw an error when trying to overwrite an existing key
    * @param map   the underlying mutable map
    * @tparam K    the type of the keys contained in the map
    * @tparam V    the type of the values assigned to keys in thw map
    */
  implicit class SafeInsertMap[K, V](map: mutable.Map[K, V]) {

    /**
      * Insert an entry into the map, performing action `f` on conflict
      * @param kv a key-value pair to insert into the map
      * @param f an action to run on conflict, instead of overwriting
      */
    def safeInsert(kv: (K, V), f: => Unit): Unit =
      if (map.contains(kv._1)) f else map += kv

    /**
      * Insert an entry into the map, throwing on error
      * @param kv a key-value pair to insert into the map
      */
    def safeInsert(kv: (K, V))(implicit fail: FailHandler): Unit =
      safeInsert(kv, fail(s"Key ${kv._1} already exists in $map."))
  }
}
