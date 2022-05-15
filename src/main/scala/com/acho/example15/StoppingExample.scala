package com.acho.example15

import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors

object StoppingExample {

  object SoftActor {
    def apply(): Behavior[String] = Behaviors.receive[String] { (context, message) =>
      context.log.info(s"Received message $message")
      message match {
        case "stop" =>
          Behaviors.stopped
        case _ =>
          Behaviors.same
      }
    }
      .receiveSignal {
        case (context, PostStop) =>
          // Kinda actor destructor, can be used for cleanup
          context.log.info("Actor stopping...")
          Behaviors.same
      }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(SoftActor(), "system")
    system ! "Hello"
    system ! "SUP"
    system ! "stop"
    system ! "Hello"
    Thread.sleep(1000)
    system.terminate()
  }

}
