package sound.audio.mixer

object MixerUtils {
  def mix(a: Array[Float], b: Array[Float], af: Float, bf: Float): Array[Float] = {
    (a.map(_ * af) zip b.map(_ * bf)).map(x => (x._1 + x._2)/2f)
  }

  def mix(m: List[(Array[Float], Float)]): Array[Float] = {
    m
      .map { case (a, f) => a.map(_ * f) }
      .reduce((m1, m2) => m1.zip(m2).map(x => (x._1 + x._2)/2f))
  }
}
