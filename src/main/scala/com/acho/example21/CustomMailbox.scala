package com.acho.example21

import akka.actor.typed.{ActorSystem, MailboxSelector}
import akka.actor.typed.scaladsl.Behaviors
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}
import com.acho.utils._

import scala.concurrent.duration.DurationInt

object CustomMailbox {

  /*
      Imagine having a ticket management system simulation.
      Tickets will have priorities and should be processed in that order.
      Top priority are support tickets P0, P1, P2, P3.
   */

  trait Command
  case class SupportTicket(content: String) extends Command

  class TicketPriorityMailbox(settings: akka.actor.ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator {
        case SupportTicket(content) if content.startsWith("P0") => 0
        case SupportTicket(content) if content.startsWith("P1") => 1
        case SupportTicket(content) if content.startsWith("P2") => 2
        case SupportTicket(content) if content.startsWith("P3") => 3
        case _ => 4
      }
    )

  def demoTicketMailbox(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val actor = context.spawn(LoggerActor[Command](), "ticket-logger",
        MailboxSelector.fromConfig("custom-ticket-mailbox"))

      actor ! SupportTicket("P1 fix UI")
      actor ! SupportTicket("P3 fix DB")
      actor ! SupportTicket("P0 fix NPE")
      actor ! SupportTicket("P2 fix button color")

      Behaviors.empty
    }

    ActorSystem(userGuardian, "system", ConfigFactory.load().getConfig("ticket-system-demo"))
      .withFiniteLife(5.seconds)
  }

  def main(args: Array[String]): Unit = {
    demoTicketMailbox()
  }

}
