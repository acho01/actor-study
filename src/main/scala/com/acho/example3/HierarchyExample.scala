package com.acho.example3

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}


// Example just shows simple actor hierarchy.
// Nested actors, have child-parent relationship.
//
// Child: Actor[akka://parent-system/user/child-actor#-212060822]
// Grand Child: Actor[akka://parent-system/user/child-actor/grand-child-actor#-2070488793]

object Child {
  def apply(): Behavior[String] = {
    Behaviors.setup(context => new Child(context))
  }
}

class Child(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  override def onMessage(msg: String): Behavior[String] = msg match {
    case "print" => {
      val grandChildRef = context.spawn(Behaviors.empty[String], "grand-child-actor")
      println(s"Grand Child: $grandChildRef")
      Behaviors.same
    }
  }
}

object Parent {
  def apply(): Behavior[String] = {
    Behaviors.setup(context => new Parent(context))
  }
}

class Parent(context: ActorContext[String]) extends AbstractBehavior[String](context){
  override def onMessage(msg: String): Behavior[String] = msg match {
    case "start" => {
      val childRef = context.spawn(Child(), "child-actor")
      println(s"Child: $childRef")
      childRef ! "print"
      Behaviors.same
    }
  }
}

object HierarchyExample extends App {
  val rootSystem: ActorSystem[String] = ActorSystem(Parent(), "parent-system")

  rootSystem ! "start"
}
