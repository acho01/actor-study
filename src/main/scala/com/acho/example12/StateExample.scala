package com.acho.example12

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object StateExample {

  object WordCountActor {
    def apply(totalCount: Int): Behavior[String] = {
      Behaviors.setup {context =>
        Behaviors.receiveMessage {message => {
          val newCount = totalCount + message.split(" ").length
          context.log.info(s"Total Count: $newCount")
          apply(newCount)
        }}
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(WordCountActor(0), "counter-system")

    system ! "hello my dear"
    system ! "boom boom"
    system ! "pow"

    Thread.sleep(1000)
    system.terminate()

  }

}
