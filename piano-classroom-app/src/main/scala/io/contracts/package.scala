package io


package object contracts {
  def initializeEmptySesionContract(): SessionContract = {
    SessionContract(
      `audio-configuration` = None,
      `vst-configuration` = VstConfiguration(
        `vst-source-directories` = List(".")
      ),
      `global` = None
    )
  }
}
