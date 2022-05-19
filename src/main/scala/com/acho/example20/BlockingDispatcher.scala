package com.acho.example20

import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors
import com.acho.utils._

import scala.concurrent.duration.DurationInt

object BlockingDispatcher {

  object BlockingActor {
    def apply(): Behavior[Int] = Behaviors.receive { (context, message) =>
      Thread.sleep(5000)
      context.log.info(s"[Blocking] received $message")
      Behaviors.same
    }
  }

  object NonBlockingActor {
    def apply(): Behavior[Int] = Behaviors.receive { (context, message) =>
      context.log.info(s"[Non-Blocking] received $message")
      Behaviors.same
    }
  }

  /*
      This demo shows how blocking operations can block non-blocking ones,
      As they are spawned within same dispatcher and same thread-pool is used to handle them
   */
  def demoBlocking(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      (1 to 100).foreach(i => {
        context.spawn(NonBlockingActor(), s"non-blocking-$i") ! i
        context.spawn(BlockingActor(), s"blocking-$i") ! i
      })

      Behaviors.same
    }

    ActorSystem(userGuardian, "system").withFiniteLife(30.seconds)
  }

  /*
      In this case we will assign separate dispatcher to the blocking actor,
      therefore all non-blocking ones will run immediately and only blocking actor will be blocked
   */
  def demoBlockingSolution(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      (1 to 100).foreach(i => {
        context.spawn(NonBlockingActor(), s"non-blocking-$i") ! i
        context.spawn(BlockingActor(), s"blocking-$i",
          DispatcherSelector.fromConfig("my-blocking-dispatcher")) ! i
      })

      Behaviors.same
    }

    ActorSystem(userGuardian, "system").withFiniteLife(30.seconds)
  }

  def main(args: Array[String]): Unit = {
//    demoBlocking()
    demoBlockingSolution()
  }

}
