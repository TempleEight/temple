package temple.collection

import scala.collection.{Iterable, MapView, mutable}

/**
  * This is a mutable view on a map: the values for each key are given at initialization, but the keys in the set are
  * given later
  *
  * Keys are activated and deactivated with the [[temple.collection.FlagMapView#flag(scala.collection.immutable.Seq)]]
  * and [[temple.collection.FlagMapView#unflag(scala.collection.immutable.Seq)]] functions respectively
  *
  * @param basis The underlying map of values for each potential key
  * @param keyDesc A description of the key type, for use in error messages
  * @tparam K the type of keys in this FlagMapView
  * @tparam V the type of values in this FlagMapView
  */
class FlagMapView[K, +V] private (val basis: Map[K, V], keyDesc: String = "flag") extends MapView[K, V] {

  /** The set of keys already enabled */
  private val flags: mutable.Set[K] = mutable.Set[K]()

  /**
    * Enable key(s) in the map
    * @param keys The keys to enable
    * @throws NoSuchElementException when a key not in the initial map is given
    * @return the FlagMapView itself
    */
  def flag(keys: K*): FlagMapView[K, V] = {
    keys.foreach { key =>
      if (!basis.contains(key))
        throw new NoSuchElementException(s"The $keyDesc $key has not been defined")
      flags += key
    }
    this
  }

  /**
    * Disable key(s) in the map
    * @param keys The keys to disable
    * @throws NoSuchElementException when a key not in the initial map is given
    * @return the FlagMapView itself
    */
  def unflag(keys: K*): FlagMapView[K, V] = {
    keys.foreach { key =>
      if (!basis.contains(key))
        throw new NoSuchElementException(s"The $keyDesc $key has not been defined")
      flags -= key
    }
    this
  }

  /** Get a view of the underlying map, containing only the enabled keys */
  override def view: MapView[K, V] = basis.view.filterKeys(flags.contains)

  /** Export a map containing only the enabled keys */
  def toMap: Map[K, V] = view.toMap

  /** Safely lookup a value in the map, only returning successfully if the key is enabled */
  def get(key: K): Option[V] = view.get(key)

  /** Get an iterator for the enabled keys */
  def iterator: Iterator[(K, V)] = view.iterator

  /** The keys that have been enabled */
  override def keys: Iterable[K] = flags.toSet
}

object FlagMapView {

  /** Create a [[temple.collection.FlagMapView]] given a basis map and optionally a description */
  def apply[K, V](basis: Map[K, V], keyDesc: String = "flag"): FlagMapView[K, V] = new FlagMapView(basis, keyDesc)

  /** Create a [[temple.collection.FlagMapView]] given key-value pairs */
  def apply[K, V](basis: (K, V)*): FlagMapView[K, V] = new FlagMapView(Map(basis: _*))

  /** Create a [[temple.collection.FlagMapView]] given a key description and key-value pairs */
  def apply[K, V](keyDesc: String)(basis: (K, V)*): FlagMapView[K, V] = new FlagMapView(Map(basis: _*), keyDesc)
}
