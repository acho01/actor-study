package com.acho.example18

import akka.actor.Cancellable
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

object Schedulers {

  object LoggerActor {
    def apply(): Behavior[String] = Behaviors.receive {(context, message) =>
      context.log.info(s"[${context.self.path}] logging message $message")
      Behaviors.same
    }
  }

  def schedulerDemo(): Unit = {
    val userGuardian = Behaviors.setup[Unit] {context =>
      val logger = context.spawn(LoggerActor(), "logger-actor")

      context.log.info("[System] Starting...")
      context.scheduleOnce(5.seconds, logger, "Hello")

      Behaviors.same
    }

    val actorSystem = ActorSystem(userGuardian, "system")

    Thread.sleep(10000)
    actorSystem.terminate()
  }

  def demoActorTimeout(): Unit = {
    val timoutActor = Behaviors.receive[String] {(context, message) =>
      val schedule = context.scheduleOnce(1.seconds, context.self, "timeout")

      message match {
        case "timeout" =>
          context.log.info("Stopping Actor")
          Behaviors.stopped
        case _ =>
          context.log.info(s"Received message $message")
          Behaviors.same
      }
    }

    val system = ActorSystem(timoutActor, "system")
    system ! "trigger"
    Thread.sleep(5000)
    system ! "this message should not get there"
  }

  object ResettingTimoutActor {

    def apply(): Behavior[String] = Behaviors.receive {(context, message) =>
      context.log.info(s"Received message $message")
      active(context.scheduleOnce(1.seconds, context.self, "timeout"))
    }

    def active(schedule: Cancellable): Behavior[String] = Behaviors.receive {(context, message) =>
      message match {
        case "timeout" =>
          context.log.info("Stopping actor")
          Behaviors.stopped
        case _ =>
          context.log.info(s"Received message $message")
          schedule.cancel()
          active(context.scheduleOnce(1.seconds, context.self, "timeout"))
      }
    }
  }

  def demoActorResetTimout(): Unit = {
    val userGuardian = Behaviors.setup[String] {context =>
      val resettingActor = context.spawn(ResettingTimoutActor(), "resetting-actor")
      resettingActor ! "start timer"
      Thread.sleep(500)
      resettingActor ! "reset"
      Thread.sleep(700)
      resettingActor ! "re-reset"
      Thread.sleep(1100)
      resettingActor ! "should not be delivered"
      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "system")
    import system.executionContext
    system.scheduler.scheduleOnce(3.seconds, () => system.terminate())
  }

  def main(args: Array[String]): Unit = {
//    schedulerDemo()
//    demoActorTimeout()
    demoActorResetTimout()
  }

}
