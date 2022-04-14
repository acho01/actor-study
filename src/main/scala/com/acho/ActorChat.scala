package com.acho

import akka._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorChat extends App {

  val system = ActorSystem("ab")
  system.registerOnTermination(() => println("Conversation Ended"))
  system.logConfiguration()
  val actorA: ActorRef = system.actorOf(Props[ChatActor], name = "a-actor")
  val actorB: ActorRef = system.actorOf(Props[ChatActor], name = "b-actor")

  trait Message

  case class ChatMessage(name: String) extends Message

  case class TriggerMessage() extends Message

  val conversation = Iterator("Hello", "Hi",
    "Sup", "Just chilling wbu?",
    "Feeling great", "Batman at 22:00?",
    "Yep, let's do it", "Cool let's meet at 20:00 and have some food before that")

  class ChatActor extends Actor {
    override def receive: Receive = {
      case TriggerMessage() =>
        println(s"Starting conversation...")
        sender() ! ChatMessage(conversation.next())
      case ChatMessage(message) =>
        println(s"[${Thread.currentThread().getName}][${self.path.name}] $message")
        Thread.sleep(500)
        if (conversation.isEmpty) system.terminate()
        else sender() ! ChatMessage(conversation.next())
    }
  }

  actorA.tell(TriggerMessage(), actorB)
}
