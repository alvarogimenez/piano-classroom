package ui.controller.settings

import java.lang.Boolean
import javafx.beans.property._
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control._
import javafx.scene.layout.{BorderPane, HBox, VBox}
import javafx.stage.Stage

import context.Context
import io.contracts.{AsioChannelConfiguration, AsioChannelEnabled, AsioConfiguration}

import scala.collection.JavaConversions._

class SettingsModel {
  val sound_asio_driver_names_ol: ObservableList[String] = FXCollections.observableArrayList[String]
  val sound_asio_driver_names: SimpleListProperty[String] = new SimpleListProperty[String](sound_asio_driver_names_ol)
  val sound_asio_driver = new SimpleStringProperty()
  var sound_asio_input_channel_config: Map[Int, BooleanProperty] = Map.empty
  var sound_asio_output_channel_config: Map[Int, BooleanProperty] = Map.empty
  var sound_asio_sample_rate: SimpleStringProperty = new SimpleStringProperty()
  var sound_asio_buffer_size: SimpleStringProperty = new SimpleStringProperty()

  def setSoundAsioDriver(d: String): Unit = sound_asio_driver.set(d)
  def getSoundAsioDriver: String = sound_asio_driver.get()
  def getSoundAsioDriverProperty: SimpleStringProperty = sound_asio_driver

  def setSoundAsioDriverNames(l: List[String]): Unit = sound_asio_driver_names_ol.setAll(l)
  def getSoundAsioDriverNames: List[String] = sound_asio_driver_names.get().toList
  def getSoundAsioDriverNamesProperty: SimpleListProperty[String] = sound_asio_driver_names

  def initSoundAsioInputChannelConfig(channels: Int): Unit = sound_asio_input_channel_config = (0 until channels).map(_ -> new SimpleBooleanProperty()).toMap
  def getSoundAsioInputChannelConfig: Map[Int, BooleanProperty] = sound_asio_input_channel_config
  def getSoundAsioInputChannelConfig(channel: Int): Boolean = sound_asio_input_channel_config(channel).get()
  def setSoundAsioInputChannelConfig(channel: Int, config: Boolean): Unit = sound_asio_input_channel_config(channel).set(config)
  def getSoundAsioInputChannelConfigProperty(channel: Int): BooleanProperty = sound_asio_input_channel_config(channel)

  def initSoundAsioOutputChannelConfig(channels: Int): Unit = sound_asio_output_channel_config = (0 until channels).map(_ -> new SimpleBooleanProperty()).toMap
  def getSoundAsioOutputChannelConfig: Map[Int, BooleanProperty] = sound_asio_output_channel_config
  def getSoundAsioOutputChannelConfig(channel: Int): Boolean = sound_asio_output_channel_config(channel).get()
  def setSoundAsioOutputChannelConfig(channel: Int, config: Boolean): Unit = sound_asio_output_channel_config(channel).set(config)
  def getSoundAsioOutputChannelConfigProperty(channel: Int): BooleanProperty = sound_asio_output_channel_config(channel)

  def setSoundAsioSampleRate(s: Int): Unit = sound_asio_sample_rate.set(s.toString)
  def getSoundAsioSampleRate: Int = sound_asio_sample_rate.get().toInt
  def getSoundAsioSampleRateProperty: SimpleStringProperty = sound_asio_sample_rate

  def setSoundAsioBufferSize(s: Int): Unit = sound_asio_buffer_size.set(s.toString)
  def getSoundAsioBufferSize: Int = sound_asio_buffer_size.get().toInt
  def getSoundAsioBufferSizeProperty: SimpleStringProperty = sound_asio_buffer_size

  def initializeFromContext(): Unit = {
    val asioDriverNames = Context.asioService.listDriverNames()
    setSoundAsioDriverNames(List(null) ++ asioDriverNames)
    Context.asioService.tryGetDriver() match {
      case Some(currentDriver) if asioDriverNames.contains(currentDriver.getName) =>
        setSoundAsioDriver(currentDriver.getName)
        initSoundAsioInputChannelConfig(currentDriver.getNumChannelsInput)
        initSoundAsioOutputChannelConfig(currentDriver.getNumChannelsOutput)
        Context.asioService.getInputChannelConfiguration()
            .foreach { case (channel, enabled) =>
              setSoundAsioInputChannelConfig(channel, enabled)
            }
        Context.asioService.getOutputChannelConfiguration()
          .foreach { case (channel, enabled) =>
            setSoundAsioOutputChannelConfig(channel, enabled)
          }

        setSoundAsioSampleRate(Context.asioService.getDriverSampleRate().toInt)
        setSoundAsioBufferSize(Context.asioService.getBufferSize())
      case _ =>
        setSoundAsioDriver(null)
    }
  }
}

class SettingsController(dialog: Stage) {
  @FXML var combobox_asio_drivers: ComboBox[String] = _
  @FXML var textfield_sample_rate: TextField = _
  @FXML var textfield_buffer_size: TextField = _
  @FXML var button_settings_panel: Button = _
  @FXML var vbox_driver_input_channel_config: VBox = _
  @FXML var vbox_driver_output_channel_config: VBox = _
  @FXML var button_enable_all: Button = _
  @FXML var button_disable_all: Button = _

  @FXML var button_close: Button = _
  @FXML var button_apply: Button = _
  @FXML var button_save_and_close: Button = _


  val model = new SettingsModel()
  model.initializeFromContext()

  var sound_checkboxes: List[CheckBox] = List.empty

  def initialize() : Unit = {
    // Bindings
    combobox_asio_drivers.itemsProperty().bindBidirectional(model.getSoundAsioDriverNamesProperty)
    combobox_asio_drivers.valueProperty().bindBidirectional(model.getSoundAsioDriverProperty)
    textfield_sample_rate.textProperty().bindBidirectional(model.getSoundAsioSampleRateProperty)
    textfield_buffer_size.textProperty().bindBidirectional(model.getSoundAsioBufferSizeProperty)

    // Item Listeners
    combobox_asio_drivers.valueProperty().addListener(new ChangeListener[String]() {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        println(s"ASIO Driver changed from $oldValue to $newValue")
        if(newValue != null) {
          changeAsioController(newValue)
        } else {
          clearAudioSettings()
        }
      }
    })

    button_settings_panel.disableProperty().bind(combobox_asio_drivers.valueProperty().isNull)
    textfield_sample_rate.disableProperty().bind(combobox_asio_drivers.valueProperty().isNull)
    textfield_buffer_size.disableProperty().bind(combobox_asio_drivers.valueProperty().isNull)
    button_settings_panel.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        Context.asioService.openSettingsPanel()
      }
    })

    button_enable_all.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        sound_checkboxes.foreach(_.selectedProperty().setValue(true))
      }
    })

    button_disable_all.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        sound_checkboxes.foreach(_.selectedProperty().setValue(false))
      }
    })


    button_close.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        dialog.close()
      }
    })

    button_apply.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        save()
      }
    })

    button_save_and_close.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        save()
        dialog.close()
      }
    })

    // Initialize component state
    updateAudioSettingsForSelectedDriver()
  }

  private def save() = {
    if(model.getSoundAsioDriver != null) {
      Context.asioService.unloadStop()
      Context.asioService.init(combobox_asio_drivers.valueProperty().get)
      Context.asioService.configureChannelBuffers(
        model.getSoundAsioInputChannelConfig.mapValues(_.get()),
        model.getSoundAsioOutputChannelConfig.mapValues(_.get())
      )
      Context.asioService.start()
    }

    val sessionSettings =
      Context.applicationSession.copy(
        `audio-configuration` = model.getSoundAsioDriver match {
          case null => None
          case soundDriverName => Some(AsioConfiguration(
            `driver-name` = soundDriverName,
            `channel-configuration` = AsioChannelConfiguration(
              input = model.getSoundAsioInputChannelConfig.map {
                case (key, value) => AsioChannelEnabled(key, value.get)
              }.toList,
              output = model.getSoundAsioOutputChannelConfig.map {
                case (key, value) => AsioChannelEnabled(key, value.get)
              }.toList
            )
          ))
        }
      )

    Context.applicationSession = sessionSettings
    context.writeApplicationSessionSettings(sessionSettings)
  }

  private def changeAsioController(driver: String) = {
    Context.asioService.unloadStop()
    Context.asioService.init(driver)

    model.initSoundAsioInputChannelConfig(Context.asioService.getAvailableInputChannels())
    model.initSoundAsioOutputChannelConfig(Context.asioService.getAvailableOutputChannels())
    (0 until Context.asioService.getAvailableInputChannels())
      .foreach { c =>
        model.setSoundAsioInputChannelConfig(c, false)
      }
    (0 until Context.asioService.getAvailableOutputChannels())
      .foreach { c =>
        model.setSoundAsioOutputChannelConfig(c, false)
      }

    model.setSoundAsioSampleRate(Context.asioService.getDriverSampleRate().toInt)
    model.setSoundAsioBufferSize(Context.asioService.getBufferSize())

    updateAudioSettingsForSelectedDriver()
  }

  private def clearAudioSettings() = {
    sound_checkboxes = List.empty
    vbox_driver_input_channel_config.getChildren.clear()
    vbox_driver_output_channel_config.getChildren.clear()

    model.getSoundAsioBufferSizeProperty.set("")
    model.getSoundAsioSampleRateProperty.set("")
  }

  private def updateAudioSettingsForSelectedDriver() = {
    sound_checkboxes = List.empty

    vbox_driver_input_channel_config.getChildren.clear()
    (0 until model.getSoundAsioInputChannelConfig.size)
      .map { c =>
        vbox_driver_input_channel_config.getChildren.add(
          createChannelEnableToggle(
            channel = c,
            booleanProperty = model.getSoundAsioInputChannelConfigProperty(c)
          )
        )
      }

    vbox_driver_output_channel_config.getChildren.clear()
    (0 until model.getSoundAsioOutputChannelConfig.size)
      .map { c =>
        vbox_driver_output_channel_config.getChildren.add(
          createChannelEnableToggle(
            channel = c,
            booleanProperty = model.getSoundAsioOutputChannelConfigProperty(c)
          )
        )
      }
  }

  private def createChannelEnableToggle(channel: Int, booleanProperty: BooleanProperty): BorderPane = {
    val borderPane = new BorderPane()
    val checkBox = new CheckBox()
    val label = new Label("Disabled")

    label.setPrefWidth(100)
    label.setMaxWidth(100)

    sound_checkboxes = sound_checkboxes :+ checkBox

    checkBox.selectedProperty().addListener(new ChangeListener[java.lang.Boolean](){
      override def changed(observable: ObservableValue[_ <: java.lang.Boolean], oldValue: java. lang.Boolean, newValue: java.lang.Boolean): Unit = {
        if(newValue) {
          label.setText("Enabled")
        } else {
          label.setText("Disabled")
        }
      }
    })
    checkBox.selectedProperty().bindBidirectional(booleanProperty)

    val hbox_enabled = new HBox()
    hbox_enabled.setSpacing(3)
    hbox_enabled.getChildren.add(checkBox)
    hbox_enabled.getChildren.add(label)

    borderPane.setPrefHeight(25)
    borderPane.setMaxHeight(25)
    borderPane.setMinHeight(25)
    borderPane.setLeft(new Label(s"Channel $channel"))
    borderPane.setRight(hbox_enabled)

    borderPane
  }
}
