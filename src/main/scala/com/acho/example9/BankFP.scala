package com.acho.example9

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

sealed trait Command

case class Withdraw(amount: Int, replyTo: ActorRef[Command]) extends Command

case class AmountWithdrawn(balanceLeft: Int, withdrawn: Int) extends Command

case class WithdrawFailed(balance: Int, amount: Int) extends Command

object BankFP {
  def apply(balance: Int): Behavior[Command] = {
    account(balance)
  }

  def account(balance: Int): Behavior[Command] = {
    Behaviors.receiveMessage {
      case Withdraw(amount, replyTo) =>
        if (balance >= amount) {
          val newBalance = balance - amount
          replyTo ! AmountWithdrawn(newBalance, amount)
          account(newBalance)
        } else {
          replyTo ! WithdrawFailed(balance, amount)
          Behaviors.same
        }
    }
  }
}

object Main {

  def apply(): Behavior[Command] = {
    Behaviors.setup(context => {
      val account = context.spawn(BankFP(100), "account")

      account ! Withdraw(40, context.self)
      account ! Withdraw(80, context.self)

      Behaviors.receiveMessage {
        case AmountWithdrawn(balanceLeft, amount) =>
          println(s"Withdrawn $amount, left $balanceLeft")
          Behaviors.same
        case WithdrawFailed(balance, amount) =>
          println(s"Failed to withdraw $amount, from $balance")
          Behaviors.stopped
      }
    })
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "account-system")
  }

}
