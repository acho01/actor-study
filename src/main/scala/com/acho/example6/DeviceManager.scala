package com.acho.example6

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.acho.example6.DeviceManager.{DeviceGroupTerminated, ReplyDeviceList, RequestDeviceList, RequestTrackDevice}

object DeviceManager {
  def apply(): Behavior[Command] =
    Behaviors.setup(context => new DeviceManager(context))

  sealed trait Command

  final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered]) extends
    DeviceManager.Command with DeviceGroup.Command

  final case class DeviceRegistered(device: ActorRef[Device.Command])

  final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList]) extends
    DeviceManager.Command with DeviceGroup.Command

  final case class ReplyDeviceList(requestId: Long, idList: Set[String])

  private final case class DeviceGroupTerminated(groupId: String) extends Command
}

class DeviceManager(context: ActorContext[DeviceManager.Command]) extends
  AbstractBehavior[DeviceManager.Command](context) {

  var groupIdToActorMap = Map.empty[String, ActorRef[DeviceGroup.Command]]

  context.log.info("DeviceManager started")

  override def onMessage(msg: DeviceManager.Command): Behavior[DeviceManager.Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(groupId, _, _) =>
        groupIdToActorMap.get(groupId) match {
          case Some(group) =>
            group ! trackMsg
          case None =>
            context.log.info("Creating device group for {}", groupId)
            val newGroup = context.spawn(DeviceGroup(groupId), s"group-$groupId")
            context.watchWith(newGroup, DeviceGroupTerminated(groupId))
            newGroup ! trackMsg
            groupIdToActorMap += groupId -> newGroup
        }
        Behaviors.same
      case listMsg @ RequestDeviceList(requestId, groupId, replyTo) =>
        groupIdToActorMap.get(groupId) match {
          case Some(group) =>
            group ! listMsg
          case None =>
            replyTo ! ReplyDeviceList(requestId, Set.empty)
        }
        this

      case DeviceGroupTerminated(groupId) =>
        context.log.info("DeviceGroup {} terminated", groupId)
        groupIdToActorMap -= groupId
        Behaviors.same

    }
}
