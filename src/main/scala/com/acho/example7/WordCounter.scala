package com.acho.example7

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.io.Source

sealed trait Command

case class StartFileCount(fileName: String) extends Command

case class StartProcessing(replyTo: ActorRef[FinalResultCount]) extends Command

case class ToProcessString(str: String, replyTo: ActorRef[ProcessedString]) extends Command

case class ProcessedString(count: Int) extends Command

case class FinalResultCount(result: Int) extends Command

object StringCounterActor {

  def apply(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      context.log.info(s"${context.self.path.name} started")
      message match {
        case ToProcessString(str, replyTo) =>
          val wordCount = str.split(" ").length
          replyTo ! ProcessedString(wordCount)
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    }
  }
}

object WordCounterActor {

  private var running = false
  private var totalLines = 0
  private var linesProcessed = 0
  private var result = 0
  private var replyToRes: Option[ActorRef[FinalResultCount]] = None

  def apply(fileName: String): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case StartProcessing(replyTo) =>
          if (running) {
            context.log.warn("Word count in progress...")
            Behaviors.same
          } else {
            running = true
            replyToRes = Some(replyTo)
            val source = Source.fromResource("test.txt")
            for (line <- source.getLines()) {
              val counterWorker = context.spawn(StringCounterActor(), s"worker-$totalLines")
              counterWorker ! ToProcessString(line, context.self)
              totalLines += 1
            }
            Behaviors.same
          }
        case ProcessedString(count) =>
          linesProcessed += 1
          result += count
          if (linesProcessed == totalLines) {
            replyToRes.get ! FinalResultCount(result)
          }
          Behaviors.same
        case _ =>
          context.log.info("Unknown message!")
          Behaviors.same
      }
    }
  }
}

object MainFileCountActor {
  def apply(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case StartFileCount(fileName) =>
          val mainCounter = context.spawn(WordCounterActor(fileName), "main-counter")
          mainCounter ! StartProcessing(context.self)
          Behaviors.same
        case FinalResultCount(result) =>
          context.log.info(s"Total number of words $result")
          Behaviors.stopped
      }
    }
  }
}

object Main extends App {
  val system = ActorSystem(MainFileCountActor(), "counter-system")
  system ! StartFileCount("test.txt")
}
