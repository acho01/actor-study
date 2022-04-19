package com.acho.example6

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.acho.example6.Device.{RecordTemperature, StopRequest, TemperatureRecorded}
import com.acho.example6.DeviceManager.{DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice}
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

  "check registration with list devices" in {
    val commandProbe = createTestProbe[DeviceRegistered]()
    val deviceGroup = spawn(DeviceGroup("group-1"))

    deviceGroup ! RequestTrackDevice("group-1", "device-1", commandProbe.ref)
    commandProbe.receiveMessage()

    deviceGroup ! RequestTrackDevice("group-1", "device-2", commandProbe.ref)
    commandProbe.receiveMessage()

    val listProbe = createTestProbe[ReplyDeviceList]()
    deviceGroup ! RequestDeviceList(1, "group-1", listProbe.ref)

    listProbe.expectMessage(ReplyDeviceList(1, Set("device-1", "device-2")))
  }

  "be able to list active devices after one device terminates" in {
    val registeredProbe = createTestProbe[DeviceRegistered]()
    val deviceGroup = spawn(DeviceGroup("group-1"))

    deviceGroup ! RequestTrackDevice("group-1", "device-1", registeredProbe.ref)
    val registeredMsg1 = registeredProbe.receiveMessage()
    val device1 = registeredMsg1.device

    deviceGroup ! RequestTrackDevice("group-1", "device-2", registeredProbe.ref)
    registeredProbe.receiveMessage()

    val listReplyProbe = createTestProbe[ReplyDeviceList]()
    deviceGroup ! RequestDeviceList(1, "group-1", listReplyProbe.ref)
    listReplyProbe.expectMessage(ReplyDeviceList(1, Set("device-1", "device-2")))

    device1 ! StopRequest()

    registeredProbe.expectTerminated(device1, registeredProbe.remainingOrDefault)
    registeredProbe.awaitAssert {
      deviceGroup ! RequestDeviceList(2, "group-1", listReplyProbe.ref)
      listReplyProbe.expectMessage(ReplyDeviceList(2, Set("device-2")))
    }
  }

}
