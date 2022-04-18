package com.acho.example6

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import com.acho.example6.Device.{ReadTemperature, RecordTemperature, RespondTemperature, TemperatureRecorded}

object Device {
  def apply(groupId: String, deviceId: String): Behavior[Command] = {
    Behaviors.setup(context => new Device(context, groupId, deviceId))
  }

  sealed trait Command

  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command

  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded]) extends Command

  final case class RespondTemperature(requestId: Long, value: Option[Double])

  final case class TemperatureRecorded(requestId: Long)
}

class Device(context: ActorContext[Device.Command],
             groupId: String, deviceId: String) extends AbstractBehavior[Device.Command](context) {

  context.log.info("Device actor {} {} started", groupId, deviceId)

  var lastTemperatureReading: Option[Double] = None

  override def onMessage(msg: Device.Command): Behavior[Device.Command] = msg match {
    case ReadTemperature(requestId, replyTo) =>
      replyTo ! RespondTemperature(requestId, lastTemperatureReading)
      Behaviors.same
    case RecordTemperature(requestId, value, replyTo) =>
      context.log.info("Recorded temperature {} {}", requestId, value)
      lastTemperatureReading = Some(value)
      replyTo ! TemperatureRecorded(requestId)
      Behaviors.same
  }

  override def onSignal: PartialFunction[Signal, Behavior[Device.Command]] = {
    case PostStop =>
      context.log.info("Device actor {} {} stopped", groupId, deviceId)
      Behaviors.same
  }
}