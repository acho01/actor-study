package com.acho.example6

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.acho.example6.DeviceManager.{DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice}
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "successfully create DeviceGroup" in {
    val managerProbe = createTestProbe[DeviceRegistered]()
    val deviceManager = spawn(DeviceManager())

    deviceManager ! RequestTrackDevice("group-1", "device-1", managerProbe.ref)
    val registeredMsg1 = managerProbe.receiveMessage()
    val device1 = registeredMsg1.device

    deviceManager ! RequestTrackDevice("group-1", "device-2", managerProbe.ref)
    val registeredMsg2 = managerProbe.receiveMessage()
    val device2 = registeredMsg2.device

    device1 should !== (device2)
  }

  "check different group and device addition with list" in {
    val managerProbe = createTestProbe[DeviceRegistered]()
    val deviceManager = spawn(DeviceManager())

    deviceManager ! RequestTrackDevice("group-1", "device-1", managerProbe.ref)
    val registeredMsg1 = managerProbe.receiveMessage()
    val device1 = registeredMsg1.device

    deviceManager ! RequestTrackDevice("group-2", "device-1", managerProbe.ref)
    val registeredMsg2 = managerProbe.receiveMessage()
    val device2 = registeredMsg2.device

    device1 should !== (device2)

    val replyListProbe = createTestProbe[ReplyDeviceList]()

    deviceManager ! RequestDeviceList(1, "group-1", replyListProbe.ref)
    replyListProbe.expectMessage(ReplyDeviceList(1, Set("device-1")))

    deviceManager ! RequestDeviceList(2, "group-2", replyListProbe.ref)
    replyListProbe.expectMessage(ReplyDeviceList(2, Set("device-1")))
  }

}
