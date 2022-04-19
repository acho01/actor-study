package com.acho.example6

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object DeviceGroup {
  def apply(groupId: String): Behavior[Command] = {
    Behaviors.setup(context => new DeviceGroup(context, groupId))
  }

  trait Command

  private final case class DeviceTerminated(device: ActorRef[Device.Command], groupId: String, deviceId: String)
    extends Command

}

class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
  extends AbstractBehavior[DeviceGroup.Command](context) {

  import DeviceGroup._
  import DeviceManager._

  private var deviceIdToActorMap = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: DeviceGroup.Command): Behavior[DeviceGroup.Command] =
    msg match {
      case RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActorMap.get(deviceId) match {
          case Some(deviceActor) =>
            replyTo ! DeviceRegistered(deviceActor)
          case None =>
            context.log.info("Registering device {} ", deviceId)
            val deviceActor = context.spawn(Device(deviceId, groupId), s"device-$deviceId")
            context.watchWith(deviceActor, DeviceTerminated(deviceActor, groupId, deviceId))
            deviceIdToActorMap += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        Behaviors.same

      case RequestTrackDevice(gId, _, _) =>
        context.log.warn("Ignoring TrackDevice request for {}. Group Id of this actor is different.", gId, groupId)
        this

      case RequestDeviceList(requestId, gId, replyTo) =>
        if (gId == groupId) {
          replyTo ! ReplyDeviceList(requestId, deviceIdToActorMap.keySet)
          Behaviors.same
        } else {
          Behaviors.unhandled
        }
      case DeviceTerminated(_, _, deviceId) =>
        context.log.info("Device {} stopped", deviceId)
        deviceIdToActorMap -= deviceId
        this

    }

  override def onSignal: PartialFunction[Signal, Behavior[DeviceGroup.Command]] = {
    case PostStop => {
      context.log.info("DeviceGroup {} stopped", groupId)
      this
    }
  }
}
