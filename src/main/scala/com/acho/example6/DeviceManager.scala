package com.acho.example6

import akka.actor.typed.delivery.ConsumerController.Delivery
import akka.actor.typed.internal.jfr.DeliveryConsumerMissing
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object DeviceManager {
  def apply(): Behavior[Command] =
    Behaviors.setup(context => ???)

  sealed trait Command

  final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered]) extends
  DeviceManager.Command with DeviceGroup.Command

  final case class DeviceRegistered(device: ActorRef[Device.Command])

  final case class RequestDeviceList(requestId: String, groupId: String, replyTo: ActorRef[ReplyDeviceList])
  final case class ReplyDeviceList(requestId: String, idList: Set[String])

}

class DeviceManager {

}
