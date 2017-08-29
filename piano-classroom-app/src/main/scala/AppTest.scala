import java.io.File
import java.util
import javax.sound.midi.{MidiMessage, ShortMessage}

import com.synthbot.audioplugin.vst.vst2.JVstHost2
import com.synthbot.jasiohost.{AsioChannel, AsioDriver, AsioDriverListener}
import sound.audio.channel.MidiChannel
import sound.audio.mixer.ChannelMix
import sound.midi.MidiListener

import scala.io.StdIn

object AppTest {

//  class MidiInputReceiver(source: MidiDevice.Info, vst: JVstHost2) extends Receiver {
//    override def send(msg: MidiMessage, timeStamp: Long): Unit = {
//      println(s"Received MIDI [${msg.getMessage}] from [${source.getName}]")
//      vst.queueMidiMessage(msg.asInstanceOf[ShortMessage])
//    }
//    override def close(): Unit = {}
//  }

  import scala.collection.JavaConversions._

//  def main(args: Array[String]): Unit = {
//    println("Hello")
//    import context.Context._
//
//    println(s"Select an ASIO Driver:")
//    asioController
//      .listDriverNames()
//      .zipWithIndex
//      .foreach { case (driver, index) =>
//        println(s"[$index] $driver")
//      }
//
//    print(s"Selected ASIO Driver:")
//    val asioDriverIndex = StdIn.readLine().toInt
//    val asioDriverName = asioController.listDriverNames()(asioDriverIndex)
//    println(s"Initializing [$asioDriverName] ...")
//
//    asioController.init(asioDriverName)
//    println(s"Available inputs: ${asioController.getAvailableInputChannels()}")
//    println(s"Available outputs: ${asioController.getAvailableOutputChannels()}")
//    println(s"Configure all available Input & Ouptuts...")
//    asioController
//      .configureChannelBuffers(
//        input = (0 until asioController.getAvailableInputChannels()).map(_ -> true).toMap,
//        output = (0 until asioController.getAvailableOutputChannels()).map(_ -> true).toMap
//      )
//
//    (0 to 5)
//        .foreach { c =>
//          println(s"Configure a new MIDI Channel ($c)...")
//          val midiChannel0 = new MidiChannel(c)
//          midiChannel0.setVstSource(new File("DSK_AkoustiK_KeyZ.dll"))
//          channelController.addChannel(midiChannel0)
//
//          println(s"Route MIDI Channel ($c) to outputs ($c)...")
//          mixerController.setChannelInOutput(ChannelMix(c, 1f), c)
//
//          println(s"Listing all Hardware MIDI Controllers...")
//          val midiWithIndex =
//          midiController.getHardwareMidiDevices()
//            .toList
//            .zipWithIndex
//
//          midiWithIndex.foreach { case ((id, device), index) =>
//            println(s"[$index] MIDI Device [${id.name}]")
//          }
//
//          val midiIndex = StdIn.readLine().toInt
//          val midiDeviceName = midiWithIndex(midiIndex)._1._1
//
//          println(s"Attach a Listener for MIDI Channel ($c) to Hardware MIDI Controller [${midiDeviceName.name}]...")
//          midiController
//            .addMidiListener(midiDeviceName, new MidiListener() {
//              override def midiReceived(msg: MidiMessage, timeStamp: Long): Unit = {
//                if(msg.isInstanceOf[ShortMessage]) {
//                  midiChannel0.queueMidiMessage(msg.asInstanceOf[ShortMessage])
//                } else {
//                  println(s"Unknown MIDI message type [$msg] [${msg.getClass.getName}]")
//                }
//              }
//            })
//        }
//
//    println(s"Start MIDI receive...")
//    midiController.attach()
//    println(s"Start ASIO controller...")
//    asioController.start()
//
//    StdIn.readLine()
//    asioController.stop()
//    midiController.detach()
//  }

//  def main(args: Array[String]): Unit = {
//    val drivers = com.synthbot.jasiohost.AsioDriver.getDriverNames().toList
//    println("Drivers:")
//    drivers.foreach(println)
//    val asio4allDriver = AsioDriver.getDriver(drivers.head)
//    val bufferSize = asio4allDriver.getBufferPreferredSize
//    val sampleRate = asio4allDriver.getSampleRate
//
//    val vst = JVstHost2.newInstance(new File("DSK_AkoustiK_KeyZ.dll"), sampleRate.toFloat, bufferSize)
//    vst.openEditor("Editor")
//
//    MidiSystem.getMidiDeviceInfo.foreach(i => println(s"Name [${i.getName}], Description [${i.getDescription}]"))
//    try {
//      MidiSystem.getMidiDeviceInfo
////        .filter(_.getName.contains("V25"))
//        .foreach { info =>
//          val device = MidiSystem.getMidiDevice(info)
//          val isHardwareMIDI = !device.isInstanceOf[Sequencer] && !device.isInstanceOf[Synthesizer]
//
//          println("-----------")
//          println(s"Name [${info.getName}], Description [${info.getDescription}], Version [${info.getVersion}], Vendor [${info.getVendor}]")
//          println(s"Max Transmitters: ${device.getMaxTransmitters}")
//          println(s"Hardware MIDI: $isHardwareMIDI")
//          println("-----------")
//
//          try {
//            device.getTransmitter.setReceiver(new MidiInputReceiver(info, vst))
//            device.open()
//            println(s"Device Opened [${info.getName}]")
//          } catch {
//            case m: MidiUnavailableException =>
//          }
//        }
//    } catch {
//      case e: Exception => e.printStackTrace()
//    }
//
//    asio4allDriver.addAsioDriverListener(new AsioDriverListener() {
//      override def bufferSwitch(sampleTime: Long, samplePosition: Long, activeChannels: util.Set[AsioChannel]) = {
//        val output = Array.ofDim[Float](vst.numOutputs, vst.getBlockSize)
//        vst.processReplacing(Array.empty, output, 512)
//        activeChannels.toList.foreach { channel =>
//          channel.write(output(0))
//        }
//      }
//
//      override def bufferSizeChanged(bufferSize: Int) =  {}
//      override def resetRequest() = {
//        println(s"Reset request")
//      }
//      override def resyncRequest() = {
//        println(s"Resync request")
//      }
//      override def latenciesChanged(inputLatency: Int, outputLatency: Int) = {}
//      override def sampleRateDidChange(sampleRate: Double) = {}
//    })
//
//    asio4allDriver.createBuffers(Set(asio4allDriver.getChannelOutput(0), asio4allDriver.getChannelOutput(1)))
//    asio4allDriver.start()
//
//    StdIn.readLine()
//    asio4allDriver.shutdownAndUnloadDriver()
//  }

}