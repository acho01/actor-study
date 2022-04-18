package com.acho.example0

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.acho.example1.Greeter.{Greet, Greeted}
import com.acho.example1.{Greeter, GreeterBot}
import org.scalatest.wordspec.AnyWordSpecLike

class GreeterAppSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike{

  "reply to greeted" in {
    val replyProbe = createTestProbe[Greeted]()
    val greeter = spawn(Greeter())
    greeter ! Greet("John", replyProbe.ref)
    replyProbe.expectMessage(Greeted("John", greeter.ref))
  }

  "greet" in {
    val greeterBot = spawn(GreeterBot(5))
    val fromProbe = createTestProbe[Greet]()
    greeterBot ! Greeted("Nick", fromProbe.ref)
    fromProbe.expectMessage(Greet("Nick", greeterBot.ref))
  }
}
