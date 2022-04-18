package com.acho.example5


import akka.actor.typed.{ActorSystem, Behavior, PostStop, PreRestart, Signal, SupervisorStrategy}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}


// If child actor fails, parent actor can have handler for that situation.
// In this case SupervisorStrategy.restart means, that if child fails, parent restarts it.
// Note that after restart exact same actor is serving incoming messages

// Child actor started
// Checking Actor[akka://supervisor-system/user/child-actor#-876577786]
// Checking Actor[akka://supervisor-system/user/child-actor#-876577786]
// supervised actor fails now...
// Child [Actor[akka://supervisor-system/user/child-actor#-876577786]] is restarting...
// Child actor started
// Checking Actor[akka://supervisor-system/user/child-actor#-876577786]

object SupervisorActor {
  def apply(): Behavior[String] = {
    Behaviors.setup(context => new SupervisorActor(context))
  }
}

class SupervisorActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  private val child = context.spawn(
    Behaviors.supervise(SupervisedActor()).onFailure(SupervisorStrategy.restart),
    "child-actor"
  )

  override def onMessage(msg: String): Behavior[String] = msg match {
    case "check-child" =>
      child ! "check"
      Behaviors.same
    case "fail-child" =>
      child ! "fail"
      Behaviors.same
  }
}

object SupervisedActor {
  def apply(): Behavior[String] = {
    Behaviors.setup(context => new SupervisedActor(context))
  }
}

class SupervisedActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("Child actor started")

  override def onMessage(msg: String): Behavior[String] = msg match {
    case "check" =>
      println(s"Checking ${context.self}")
      Behaviors.same
    case "fail" =>
      println("supervised actor fails now...")
      throw new Exception(s"[${context.self}] failed!")
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PreRestart =>
      println(s"Child [${context.self}] is restarting...")
      Behaviors.same
    case PostStop => {
      println(s"Child [${context.self}] Stopped")
      Behaviors.same
    }
  }
}

object SupervisorFailHandle extends App {
  val system: ActorSystem[String] = ActorSystem(SupervisorActor(), "supervisor-system")

  system ! "check-child"
  system ! "check-child"
  system ! "fail-child"
  system ! "check-child"

}
