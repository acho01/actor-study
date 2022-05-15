package com.acho.example13

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.acho.example13.ChildActors.Parent.{CreateChild, StopChild, TellChild}

object ChildActors {

  object Parent {

    trait Command

    case class CreateChild(name: String) extends Command

    case class TellChild(message: String) extends Command

    case class StopChild() extends Command

    def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.log.info(s"Creating child $name")
          val childRef = context.spawn(Child(), name)
          active(childRef)
      }
    }

    def active(childRef: ActorRef[String]): Behavior[Command] =
      Behaviors.receive { (context, message) =>
        message match {
          case TellChild(msg) =>
            context.log.info(s"Telling child ${childRef.path.name} $msg")
            childRef ! msg
            Behaviors.same
          case StopChild() =>
            context.log.info(s"Stopping child ${childRef.path}")
            context.stop(childRef)
            apply()
          case _ =>
            context.log.info("Unknown Message")
            Behaviors.same
        }
      }
  }

  object Child {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"Child received message: $message")
      Behaviors.same
    }
  }

  def userGuardianBehavior(): Behavior[NotUsed] = Behaviors.setup { context =>
    val parent = context.spawn(Parent(), "parent")
    parent ! CreateChild("child-1")
    parent ! TellChild("Hi Child")
    parent ! StopChild()
    parent ! CreateChild("child-2")
    parent ! TellChild("Ola Amigo")
    Behaviors.empty
  }

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(userGuardianBehavior(), "system")
    Thread.sleep(1000)
    actorSystem.terminate()
  }

}
