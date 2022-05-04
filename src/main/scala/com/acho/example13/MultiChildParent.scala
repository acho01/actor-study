package com.acho.example13

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.acho.example13.MultiChildParent.Parent.{CreateChild, TellChildren}

object MultiChildParent {

  object Parent {

    trait Command

    case class CreateChild(name: String) extends Command

    case class TellChildren(message: String) extends Command

    def apply(children: List[ActorRef[String]]): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.log.info(s"Creating child $name")
          val childRef = context.spawn(Child(), name)
          apply(childRef :: children)
        case TellChildren(msg) =>
          context.log.info(s"Telling children $msg")
          children.foreach(child => child ! msg)
          Behaviors.same
        case _ =>
          context.log.info("Unknown Message")
          Behaviors.same
      }
    }
  }

  object Child {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"Child [${context.self.path.name}] received message: $message")
      Behaviors.same
    }
  }

  def userGuardianBehavior(): Behavior[NotUsed] = Behaviors.setup { context =>
    val parent = context.spawn(Parent(List()), "parent")
    parent ! CreateChild("child-1")
    parent ! CreateChild("child-2")
    parent ! CreateChild("child-3")
    parent ! CreateChild("child-4")
    parent ! TellChildren("Hello")
    Behaviors.empty
  }

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(userGuardianBehavior(), "system")
    Thread.sleep(1000)
    actorSystem.terminate()
  }
}
