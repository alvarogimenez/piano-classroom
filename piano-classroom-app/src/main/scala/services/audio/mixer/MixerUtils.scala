package services.audio.mixer

object MixerUtils {
  def mix(a: Array[Float], b: Array[Float], af: Float, bf: Float): Array[Float] = {
    (a.map(_ * af) zip b.map(_ * bf)).map(x => (x._1 + x._2)/2f)
  }

  def mix(m: List[(Array[Float], Float)]): Array[Float] = {
    val length = m.head._1.length
    val n = m.length
    val result = Array.ofDim[Float](length)
    var i = 0
    while(i < length) {
      result.update(i, {
        var j = 0
        var sum = 0f
        while(j < n) {
          val el = m(j)
          sum += + el._1(i) * el._2
          j += 1
        }
        sum
      })
      i += 1
    }
    result
  }
}
