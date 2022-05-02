package com.acho.example8

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import com.acho.example8.ChatRoom.GetSession

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ChatRoom {

  sealed trait RoomCommand

  final case class GetSession(screenName: String, replyTo: ActorRef[SessionEvent]) extends RoomCommand

  private final case class PublishSessionMessage(screenName: String, message: String) extends RoomCommand

  sealed trait SessionEvent

  final case class SessionGranted(handle: ActorRef[PostMessage]) extends SessionEvent

  final case class SessionDenied(reason: String) extends SessionEvent

  final case class MessagePosted(screenName: String, message: String) extends SessionEvent


  sealed trait SessionCommand

  final case class PostMessage(message: String) extends SessionCommand

  private final case class NotifyClient(messagePosted: MessagePosted) extends SessionCommand

  def apply(): Behavior[RoomCommand] =
    chatRoom(List.empty)

  private def chatRoom(sessions: List[ActorRef[SessionCommand]]): Behavior[RoomCommand] = {
    Behaviors.receive { (context, message) =>
      message match {
        case GetSession(screenName, client) =>
          val sess = context.spawn(session(context.self, screenName, client),
            name = URLEncoder.encode(screenName, StandardCharsets.UTF_8.name))
          client ! SessionGranted(sess)
          chatRoom(sess :: sessions)
        case PublishSessionMessage(screenName, message) => {
          val notification = NotifyClient(MessagePosted(screenName, message))
          sessions.foreach(_ ! notification)
          Behaviors.same
        }
      }
    }
  }

  private def session(
                       room: ActorRef[PublishSessionMessage],
                       screenName: String,
                       client: ActorRef[SessionEvent]): Behavior[SessionCommand] = {
    Behaviors.receive { (_, message) =>
      message match {
        case PostMessage(message) =>
          room ! PublishSessionMessage(screenName, message)
          Behaviors.same
        case NotifyClient(messagePosted) =>
          client ! messagePosted
          Behaviors.same
      }
    }
  }
}

object Client {

  import ChatRoom._

  def apply(): Behavior[SessionEvent] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case SessionGranted(handle) =>
          handle ! PostMessage("Hello Boom!")
          Behaviors.same
        case MessagePosted(screenName, message) =>
          context.log.info("message has been posted by '{}': {}", screenName, message)
          Behaviors.stopped
      }
    }
}

object Main {
  def apply(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val chatRoom = context.spawn(ChatRoom(), "chatroom")
      val client = context.spawn(Client(), "client")
      context.watch(client)
      chatRoom ! GetSession("client-1", client)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "ChatRoomSystem")
  }
}