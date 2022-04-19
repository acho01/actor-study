package com.acho.example6

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.acho.example6.Device.{ReadTemperature, RecordTemperature, RespondTemperature, TemperatureRecorded}
import com.acho.example6.DeviceManager.{DeviceRegistered, RequestTrackDevice}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class DeviceGroupSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "successfully register new device" in {
    val commandProbe = createTestProbe[DeviceRegistered]()
    val deviceGroup = spawn(DeviceGroup("group-1"))

    deviceGroup ! RequestTrackDevice("group-1", "device-1", commandProbe.ref)
    val registeredMsg1 = commandProbe.receiveMessage()
    val deviceActor1 = registeredMsg1.device

    deviceGroup ! RequestTrackDevice("group-1", "device-2", commandProbe.ref)
    val registeredMsg2 = commandProbe.receiveMessage()
    val deviceActor2 = registeredMsg2.device

    deviceActor1 should !== (deviceActor2)

    val recorderProbe = createTestProbe[TemperatureRecorded]()

    deviceActor1 ! RecordTemperature(1, 10.0, recorderProbe.ref)
    recorderProbe.expectMessage(TemperatureRecorded(1))

    deviceActor2 ! RecordTemperature(2, 20.5, recorderProbe.ref)
    recorderProbe.expectMessage(TemperatureRecorded(2))
  }

  "ignore wrong groupId requests" in {
    val commandProbe = createTestProbe[DeviceRegistered]()
    val deviceGroup = spawn(DeviceGroup("group-1"))

    deviceGroup ! RequestTrackDevice("group-2", "device-1", commandProbe.ref)

    commandProbe.expectNoMessage(1000.milli)
  }

  "return existing device for already registered actor" in {
    val commandProbe = createTestProbe[DeviceRegistered]()
    val deviceGroup = spawn(DeviceGroup("group-1"))

    deviceGroup ! RequestTrackDevice("group-1", "device-1", commandProbe.ref)
    val registeredMsg1 = commandProbe.receiveMessage()
    val device1 = registeredMsg1.device

    deviceGroup ! RequestTrackDevice("group-1", "device-1", commandProbe.ref)
    val registeredMsg2 = commandProbe.receiveMessage()
    val device2 = registeredMsg2.device

    device1 should === (device2)
  }

}
