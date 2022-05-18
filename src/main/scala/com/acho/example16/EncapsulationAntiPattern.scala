package com.acho.example16

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.mutable.{Map => MutableMap}

/*
  Never Pass Mutable State To Other Actors
  Never Pass Context Reference To Other Actors

  Hence actor model, and particularly akka does not provide strict encapsulation by default,
  We still have to take care not to leak mutable resources to other actors.
  When used correctly with immutable only messages actor model becomes the real encapsulated monster :)
 */

object EncapsulationAntiPattern {

  trait AccountCommand
  case class Deposit(cardId: String, amount: Double) extends AccountCommand
  case class Withdraw(cardId: String, amount: Double) extends AccountCommand
  case class CreateCard(cardId: String) extends AccountCommand
  case class PrintBalance(cardId: String) extends AccountCommand
  case object CheckCardStatuses extends AccountCommand


  trait CreditCardCommand
  case class AttachToAccount(balances: MutableMap[String, Double], cardMap: MutableMap[String, ActorRef[CreditCardCommand]]) extends CreditCardCommand
  case object CheckStatus extends CreditCardCommand

  object NaiveBankAccount {
    def apply(): Behavior[AccountCommand] = Behaviors.setup { context =>
      val accountBalances: MutableMap[String, Double] = MutableMap()
      val cardMap: MutableMap[String, ActorRef[CreditCardCommand]] = MutableMap()

      Behaviors.receiveMessage {
        case CreateCard(cardId) =>
          context.log.info(s"Creating card $cardId")
          val creditCard = context.spawn(CreditCard(cardId), cardId)

          // This is a silly bug but it demonstrates the collapse of encapsulation when having mutable data
          // Assume you just add 5 amount to card when creating
          accountBalances += cardId -> 5

          creditCard ! AttachToAccount(accountBalances, cardMap)
          Behaviors.same
        case Deposit(cardId, amount) =>
          val oldBalance: Double = accountBalances.getOrElse(cardId, 0)
          context.log.info(s"Depositing $amount on card $cardId")
          accountBalances += cardId -> (oldBalance+amount)
          Behaviors.same
        case Withdraw(cardId, amount) =>
          val oldBalance: Double = accountBalances.getOrElse(cardId, 0)
          context.log.info(s"Withdrawing $amount on card $cardId")
          if (oldBalance < amount) {
            context.log.warn(s"Not enough balance on card $cardId")
          }
          accountBalances += cardId -> (oldBalance-amount)
          Behaviors.same
        case PrintBalance(cardId) =>
          context.log.info(s"Balance on card $cardId is ${accountBalances.get(cardId)}")
          Behaviors.same
        case CheckCardStatuses =>
          context.log.info("Checking all card statuses")
          cardMap.values.foreach(card => card ! CheckStatus)
          Behaviors.same
      }
    }
  }

  object CreditCard {
    def apply(cardId: String): Behavior[CreditCardCommand] = Behaviors.receive {(context, message) =>
      message match {
        case AttachToAccount(balances, cards) =>
          context.log.info(s"[$cardId] Attaching to bank account")
          balances += cardId -> 0
          cards += cardId -> context.self
          Behaviors.same
        case CheckStatus =>
          context.log.info(s"[$cardId] Checking card status : OK")
          Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardian: Behavior[Unit] = Behaviors.setup {context =>
      val bankAccount = context.spawn(NaiveBankAccount(), "bank-account")

      bankAccount ! CreateCard("silver")
      bankAccount ! CreateCard("gold")

      bankAccount ! Deposit("silver", 100)
      bankAccount ! Deposit("silver", 100)
      bankAccount ! Withdraw("silver", 50)
      bankAccount ! PrintBalance("silver")
      bankAccount ! PrintBalance("gold")
      bankAccount ! CheckCardStatuses
      Behaviors.same
    }

    val actorSystem = ActorSystem(userGuardian, "system")

    Thread.sleep(1000)
    actorSystem.terminate()
  }

}
