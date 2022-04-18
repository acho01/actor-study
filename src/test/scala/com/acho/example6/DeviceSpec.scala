package com.acho.example6

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import Device._

  "Device actor" must {
    "reply with empty temperature if no temperature is set" in {
      val commanderProbe = createTestProbe[RespondTemperature]()
      val device = spawn(Device("1", "2"))

      device ! ReadTemperature("5", commanderProbe.ref)
      commanderProbe.expectMessage(RespondTemperature("5", None))
    }
  }
}
