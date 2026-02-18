package util

object MapReverse {

  def reverseMap[K, V](map: Map[K, V]): Map[V, K] = map.map { case(key, value) =>
      value -> key
  }

}
