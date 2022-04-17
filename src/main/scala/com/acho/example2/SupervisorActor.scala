package com.acho.example2

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

// Whenever some actor fails, supervisor actor re-creates the actor,
// hence leading to creating different thread for the failed actor.
// You can spot this behaviour with running example below.
// After divide by 0 error, thread will be changed "automatically" by supervisor actor.

object DividerActor {
  final case class Operands(left: Int, right: Int)

  def apply(): Behavior[Operands] = {
    Behaviors.receive {(context, message) =>
      val res = message.left/message.right
      context.log.info("{} divided by {} is {}!",message.left, message.right, res)
      Behaviors.same
    }
  }
}

object SupervisorActor extends App {
  val system: ActorSystem[DividerActor.Operands] = ActorSystem(DividerActor(), "DividerSystem")

  system ! DividerActor.Operands(10, 5)
  system ! DividerActor.Operands(50, 5)
  system ! DividerActor.Operands(16, 0)
  system ! DividerActor.Operands(10, 2)
  system ! DividerActor.Operands(16, 2)
}
