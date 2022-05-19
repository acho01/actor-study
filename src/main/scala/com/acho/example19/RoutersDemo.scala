package com.acho.example19

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import com.acho.utils._

import scala.concurrent.duration._

object RoutersDemo {

  def demoPoolRouter(): Unit = {
    val loggerWorker = LoggerActor[String]()
    val poolRouter = Routers.pool(5)(loggerWorker).withBroadcastPredicate(_.length>13)

    val userGuardian = Behaviors.setup[Unit] {context =>
      val poolActor = context.spawn(poolRouter, "pool-router")

      (1 to 12).foreach(i => poolActor ! s"worker-task-$i")

      Behaviors.empty
    }

    ActorSystem(userGuardian, "system").withFiniteLife(1.seconds)
  }

  def main(args: Array[String]): Unit = {
    demoPoolRouter()
  }

}
