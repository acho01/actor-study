package com.acho.example14

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}


/*
  Client ------ Computational Task -------> WCC ------ Computational Task --------> WCW (child worker)
  Client ------ Computational Result  <------- WCC ------ Computational Result <-------- WCW (child worker)
 */

object ChildActorWordCounter {

  trait ControllerProtocol

  trait WorkerProtocol

  trait ClientProtocol

  // Controller messages
  case class Initialize(numWorkers: Int) extends ControllerProtocol

  case class WordCountTask(text: String, replyTo: ActorRef[ClientProtocol]) extends ControllerProtocol

  case class WordCountWorkerReply(id: Int, count: Int) extends ControllerProtocol

  // Worker messages
  case class WordCountWorkerTask(id: Int, text: String) extends WorkerProtocol

  // Client messages
  case class WordCountReply(count: Int) extends ClientProtocol

  object WordCounterController {
    def apply(): Behavior[ControllerProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case Initialize(numWorkers) =>
          context.log.info(s"[controller] initializing with $numWorkers workers")
          val workers = for {
            i <- 1 to numWorkers
          } yield context.spawn(WordCounterWorker(context.self), s"worker-$i")

          active(workers, 0, 0, Map())
        case _ =>
          context.log.info("[controller] Message not supported!")
          Behaviors.same
      }
    }

    def active(
                workerRefs: Seq[ActorRef[WorkerProtocol]],
                currentWorkerIndex: Int,
                currentTaskId: Int,
                requestMap: Map[Int, ActorRef[ClientProtocol]]
              ): Behavior[ControllerProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case WordCountTask(text, replyTo) =>
          context.log.info(s"[controller] received $text, sending it to worker $currentWorkerIndex")
          val task = WordCountWorkerTask(currentTaskId, text)
          val worker = workerRefs(currentWorkerIndex)
          worker ! task
          val newWorkerIndex = (currentWorkerIndex + 1) % workerRefs.length
          val newTaskId = currentTaskId + 1
          val newRequestMap = requestMap + (currentTaskId -> replyTo)
          active(workerRefs, newWorkerIndex, newTaskId, newRequestMap)
        case WordCountWorkerReply(id, count) =>
          context.log.info(s"[controller] received $id task with count $count form worker")
          val replyTo = requestMap(id)
          replyTo ! WordCountReply(count)
          active(workerRefs, currentWorkerIndex, currentTaskId, requestMap - id)
        case _ =>
          context.log.info("[controller] Message not supported!")
          Behaviors.same
      }
    }

  }

  object WordCounterWorker {
    def apply(controllerRef: ActorRef[ControllerProtocol]): Behavior[WorkerProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case WordCountWorkerTask(id, text) =>
          context.log.info(s"[${context.self.path}] received $id $text request to process...")
          val res = text.split(" ").length
          controllerRef ! WordCountWorkerReply(id, res)
          Behaviors.same
      }
    }
  }

  object Client {
    def apply(): Behavior[ClientProtocol] = active()

    def active(totalWords: Int = 0): Behavior[ClientProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case WordCountReply(count) =>
          val updatedCount = count + totalWords
          context.log.info(s"Client received count: $count, total count is $updatedCount")
          active(updatedCount)
      }
    }
  }

  def testWordCounter(): Unit = {
    val userGuardian: Behavior[ClientProtocol] = Behaviors.setup { context =>
      val client = context.spawn(Client(), "client")
      val controller = context.spawn(WordCounterController(), "controller")

      controller ! Initialize(5)
      controller ! WordCountTask("Hello my dear friend", client.ref)
      controller ! WordCountTask("Boom boom boom", client.ref)
      controller ! WordCountTask("Bingo", client.ref)

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "system")

    Thread.sleep(1000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
    testWordCounter()
  }

}
