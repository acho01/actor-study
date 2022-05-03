package com.acho.example11

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object SimpleActor {

  object Actor {
    def apply(): Behavior[String] = {
      Behaviors.receive { (context, message) =>
        context.log.info(s"Message received $message")
        Behaviors.same
      }
    }
  }

  object Actor_V2 {
    def apply(): Behavior[String] = {
      Behaviors.setup { context =>
        // This place is for actor's private state and any setup needed

        Behaviors.receiveMessage { message =>
          context.log.info(s"Message received $message")
          Behaviors.same
        }
      }
    }
  }

  def demoSimpleActor(): Unit = {
    val actorSystem = ActorSystem(Actor(), "simple-actor-system")

    actorSystem ! "Boom"

    Thread.sleep(1000)
    actorSystem.terminate()
  }

  def main(args: Array[String]): Unit = {
    demoSimpleActor()
  }
}
