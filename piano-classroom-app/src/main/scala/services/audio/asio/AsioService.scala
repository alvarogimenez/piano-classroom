package services.audio.asio

import java.util

import com.synthbot.jasiohost.{AsioChannel, AsioDriver, AsioDriverListener}
import services.audio.mixer.MixerOwner

import scala.collection.JavaConversions._
import scala.util.Try

class AsioService(mixerOwner: MixerOwner) {
  var driver: Option[AsioDriver] = None

  def listDriverNames(): List[String] =
    AsioDriver.getDriverNames.toList

  def init(driverName: String): Unit = {
    driver = Try(AsioDriver.getDriver(driverName)).toOption

    getDriver().addAsioDriverListener(new AsioDriverListener() {
      override def bufferSwitch(sampleTime: Long, samplePosition: Long, activeChannels: util.Set[AsioChannel]) = {
        val output = mixerOwner.pull(getDriverSampleRate(), getBufferSize())
        activeChannels
          .toList
          .filterNot(_.isInput)
          .zipWithIndex
          .foreach { case (channel, index) =>
            channel.write(output.getOrElse(index, Array.fill[Float](getBufferSize())(0)))
          }
      }

      override def bufferSizeChanged(bufferSize: Int) =  {}
      override def resetRequest() = {
        println(s"Reset request")
      }
      override def resyncRequest() = {
        println(s"Resync request")
      }
      override def latenciesChanged(inputLatency: Int, outputLatency: Int) = {}
      override def sampleRateDidChange(sampleRate: Double) = {}
    })
  }

  def tryGetDriver(): Option[AsioDriver] = driver

  def getDriver(): AsioDriver = {
    driver.getOrElse(throw new Exception("Driver has not been initialized yet"))
  }

  def getDriverSampleRate() = {
    getDriver().getSampleRate
  }

  def getBufferSize() = {
    getDriver().getBufferPreferredSize
  }

  def getAvailableOutputChannels() = {
    getDriver().getNumChannelsOutput
  }

  def getAvailableInputChannels() = {
    getDriver().getNumChannelsInput
  }

  def openSettingsPanel() = {
    getDriver().openControlPanel()
  }

  def getInputChannelConfiguration(): Map[Int, Boolean] = {
    (0 until getDriver().getNumChannelsInput)
        .map { c =>
          c -> getDriver().getChannelInput(c).isActive
        }.toMap
  }

  def getOutputChannelConfiguration(): Map[Int, Boolean] = {
    (0 until getDriver().getNumChannelsOutput)
      .map { c =>
        c -> getDriver().getChannelOutput(c).isActive
      }.toMap
  }

  def configureChannelBuffers(input: Map[Int, Boolean], output: Map[Int, Boolean]): Unit = {
    getDriver().createBuffers(
      input.filter  { case (k, v) => v }.keys.map(getDriver().getChannelInput).toSet ++
      output.filter { case (k, v) => v }.keys.map(getDriver().getChannelOutput).toSet
    )
  }

  def start() = {
    getDriver().start()
  }

  def stop() = {
    getDriver().stop()
  }

  def unloadStop() = {
    driver.foreach(_.shutdownAndUnloadDriver())
    driver = None
  }

}
