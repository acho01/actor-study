package com.acho.example17

import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

object Supervision {

  object CriminalWordCounter {
    def apply(): Behavior[String] = Behaviors.setup {context =>
      context.log.info(s"[CHILD] starting... ${context.self}")
      active()
    }

    def active(totalCount: Int = 0): Behavior[String] = Behaviors.receive {(context, message) =>
      val count = message.split(" ").length
      val updatedTotal = totalCount + count
      context.log.info(s"Word $message, Count $count, updating total count to $updatedTotal")

      if (message.startsWith("A")) throw new RuntimeException("Booom")
      if (message.startsWith("B")) throw new NullPointerException()

      active(updatedTotal)
    }
  }

  def demoCrash(): Unit = {
    val userGuardian: Behavior[Unit] = Behaviors.setup {context =>
      val counter = context.spawn(CriminalWordCounter(), "word-counter")
      counter ! "Hello There"
      counter ! "A long river is flowing "
      Behaviors.same
    }

    val system = ActorSystem(userGuardian, "system")

    Thread.sleep(1000)
    system.terminate()
  }

  def demoWithParent(): Unit = {
    val parentBehavior: Behavior[String] = Behaviors.setup {context =>
      val childCounter = context.spawn(CriminalWordCounter(), "word-counter")
      context.watch(childCounter)

      Behaviors.receiveMessage[String] { message =>
        childCounter ! message
        Behaviors.same
      }
        .receiveSignal {
          case (context, Terminated(childRef)) =>
            context.log.info(s"Child Failed ${childRef.path.name}")
            Behaviors.same
        }
    }

    val userGuardian: Behavior[String] = Behaviors.setup {context =>
      val parent = context.spawn(parentBehavior, "parent")

      parent ! "Hello There"
      parent ! "A long river is flowing "
      parent ! "Sup... "

      Behaviors.same
    }

    val system = ActorSystem(userGuardian, "system")

    Thread.sleep(1000)
    system.terminate()
  }

  def demoSupervisionWithRestart(): Unit = {
    val parentBehavior: Behavior[String] = Behaviors.setup {context =>

      val childBehavior = Behaviors.supervise(CriminalWordCounter())
        .onFailure[RuntimeException](SupervisorStrategy.restartWithBackoff(1.second, 5.minute, 0.1))


      val child = context.spawn(childBehavior, "word-counter")
      context.watch(child)

      Behaviors.receiveMessage[String] { message =>
        child ! message
        Behaviors.same
      }
        .receiveSignal {
          case (context, Terminated(childRef)) =>
            context.log.info(s"Child Failed ${childRef.path.name}")
            Behaviors.same
        }
    }

    val userGuardian: Behavior[String] = Behaviors.setup {context =>
      val parent = context.spawn(parentBehavior, "parent")

      parent ! "Hello There"
      parent ! "A long river is flowing "
      parent ! "Sup... "

      Behaviors.same
    }

    val system = ActorSystem(userGuardian, "system")

    Thread.sleep(5000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
//    demoCrash()
//    demoWithParent()
    demoSupervisionWithRestart()
  }
}
