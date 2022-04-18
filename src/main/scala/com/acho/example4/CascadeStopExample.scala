package com.acho.example4

import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}


// Example shows cascading stop nature of Actors
// When Root actor is messaged to stop, first all children are stopped, and then root stops

//  Root started
//  Child A started
//  Child B started
//
//  Child B Stopped
//  Child A Stopped
//  Root Stopped

object RootActor {
  def apply(): Behavior[String] = {
    Behaviors.setup(context => new RootActor(context))
  }
}

class RootActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("Root started")
  context.spawn(ChildActorA(), "child-actor-a")

  override def onMessage(msg: String): Behavior[String] = msg match {
    case "stop" =>
      Behaviors.stopped
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop => {
      println("Root Stopped")
      Behaviors.same
    }
  }
}

object ChildActorA {
  def apply(): Behavior[String] = {
    Behaviors.setup(context => new ChildActorA(context))
  }
}

class ChildActorA(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("Child A started")
  context.spawn(ChildActorB(), "child-actor-b")

  override def onMessage(msg: String): Behavior[String] = {
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop => {
      println("Child A Stopped")
      Behaviors.same
    }
  }
}

object ChildActorB {
  def apply(): Behavior[String] = {
    Behaviors.setup(context => new ChildActorB(context))
  }
}

class ChildActorB(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("Child B started")

  override def onMessage(msg: String): Behavior[String] = {
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop => {
      println("Child B Stopped")
      Behaviors.same
    }
  }
}

object CascadeStopExample extends App {
  val rootSystem = ActorSystem(RootActor(), "root-system")

  rootSystem ! "stop"
}
