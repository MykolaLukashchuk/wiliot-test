package actors

import actors.BindingActor.Msg
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.util.Timeout
import domain.AssetObject.DeviceId
import domain.DeviceObject.{Payload, Timestamp}
import domain.{AssetObject, Min, Reply}
import repo.AssetService

import java.util.concurrent.TimeUnit

object BindingActor {

  val TypeKey: EntityTypeKey[Msg] = EntityTypeKey[Msg]("BindingActor")

  def entityId(deviceId: DeviceId) = s"Binding_$deviceId"

  private implicit val timeout: Timeout = Timeout(10, TimeUnit.SECONDS)

  sealed trait Msg
  case class DeviceMsgs(deviceId: DeviceId, values: Seq[(Timestamp, Payload)], replyTo: ActorRef[Reply]) extends Msg

  def initShard(implicit assetService: AssetService, system: ActorSystem[_]) = {
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      BindingActor(entityContext.entityId)
    })
  }

  def apply(deviceId: String)(implicit assetService: AssetService): Behavior[Msg] =
    Behaviors.setup(context => new BindingActor(context).getBehavior(deviceId.toInt))

}

class BindingActor(ctx: ActorContext[Msg])(implicit assetService: AssetService) {

  import BindingActor._

  private def getBehavior(deviceId: DeviceId): Behavior[Msg] = {
    val assetId: Int = assetService.getAssetIdByDeviceId(deviceId) match {
      case Some(id) => id
      case None => assetService.create(AssetObject(deviceId, Min, deviceId)).id
    }

    val sharding: ClusterSharding = ClusterSharding(ctx.system)

    Behaviors.receiveMessage[Msg] {
      case DeviceMsgs(deviceId, values, replyTo) =>
        val target = sharding.entityRefFor(AssetActor.TypeKey, assetId.toString)
        target ! AssetActor.DeviceMsgs(deviceId, values, replyTo)
        Behaviors.same
    }
  }
}
