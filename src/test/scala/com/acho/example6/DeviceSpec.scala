package com.acho.example6

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

import scala.Some

class DeviceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  import Device._

  "Device actor" must {
    "reply with empty temperature if no temperature is set" in {
      val commanderProbe = createTestProbe[RespondTemperature]()
      val device = spawn(Device("1", "2"))

      device ! ReadTemperature(5, commanderProbe.ref)
      commanderProbe.expectMessage(RespondTemperature(5, None))
    }

    "reply with latest temperature record" in {
      val readProbe = createTestProbe[RespondTemperature]()
      val recordProbe = createTestProbe[TemperatureRecorded]()

      val device = spawn(Device("group-12", "device-2"))
      device ! RecordTemperature(1, 38, recordProbe.ref)
      device ! ReadTemperature(1, readProbe.ref)

      recordProbe.expectMessage(TemperatureRecorded(1))

      val recordedTemp1 = readProbe.receiveMessage()
      recordedTemp1.requestId should === (1)
      recordedTemp1.value should === (Some(38))

      device ! RecordTemperature(2, 12, recordProbe.ref)
      device ! ReadTemperature(2, readProbe.ref)

      recordProbe.expectMessage(TemperatureRecorded(2))

      val recordedTemp2 = readProbe.receiveMessage()
      recordedTemp2.requestId should === (2)
      recordedTemp2.value should === (Some(12))
    }
  }
}
