package com.acho.example10

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

/*
  An example of the failure of traditional threading model.
  Whenever an exception is thrown in one of the workers, the whole process is failed
  and exception is propagated up to the call stack.
 */

object ThreadModelLimitation {

  implicit val executorContext: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  val futures = (0 to 9)
    .map(i => BigInt(100000 * i) until BigInt(100000 * (i + 1)))
    .map(range => Future {
      if (range.contains(BigInt(457999))) throw new RuntimeException("ERROR NUMBER!!!")
      range.sum
    })

  val sumFutures = Future.reduceLeft(futures)(_ + _)

  def main(args: Array[String]): Unit = {
    sumFutures.onComplete(println)
  }

}
