package actors

import actors.AssetActor._
import akka.actor.PoisonPill
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import domain.AssetObject.DeviceId
import domain.DeviceObject.Payload
import domain._
import repo.AssetRepository
import service.PublishService

import scala.language.postfixOps

object AssetActor {
  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("AssetActor")

  def initShard(implicit
      repository: AssetRepository,
      system: ActorSystem[_],
      publishService: PublishService
  ) =
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      AssetActor(entityContext.entityId)
    })

  def entityId(assetId: String): String = s"Asset_$assetId"

  sealed trait Command
  case class DeviceMsgs(deviceId: DeviceId, values: Seq[Payload], replyTo: ActorRef[Reply])
      extends Command
  case class UpdateMetadata(metadata: Metadata) extends Command
  case object AssetDelete                       extends Command

  sealed trait Event
  type Interval = Int
  case class IntervalUpdatePersisted(entityId: String)                     extends Event
  case class MetadataUpdatePersisted(entityId: String, metadata: Metadata) extends Event

  sealed trait State {
    def assetObject: AssetObject
    def n: Interval
  }
  case class ActiveState(assetObject: AssetObject, n: Interval = 0) extends State

  def apply(
      assetId: String
  )(implicit repository: AssetRepository, publishService: PublishService): Behavior[Command] =
    new AssetActor(assetId).getBehavior()
}

class AssetActor(assetId: String)(implicit
    repository: AssetRepository,
    publishService: PublishService
) {

  def getBehavior(): Behavior[Command] = {

    Behaviors.setup { ctx =>
      val entityId: String = AssetActor.entityId(assetId)
      EventSourcedBehavior[Command, Event, State](
        PersistenceId.ofUniqueId(entityId),
        ActiveState(
          repository
            .find(assetId.toInt)
            .getOrElse(throw new RuntimeException("AssetObject not found"))
        ),
        (state, command) => handleCommand(entityId, state, command, ctx),
        (state, event) => handleEvent(state, event, ctx)
      )
    }
  }

  private def handleCommand(entityId: String, state: State, command: Command, ctx: ActorContext[_]): Effect[Event, State] =
    state match {
      case ActiveState(_, _) =>
        command match {
          case DeviceMsgs(_, values, replyTo) =>
            Effect
              .persist(IntervalUpdatePersisted(entityId))
              .thenRun { newState =>
                replyTo ! Success
                if (values.nonEmpty) publishService.publish(toAssetEvent(newState, values))
              }
          case UpdateMetadata(metadata) =>
            Effect
              .persist(MetadataUpdatePersisted(entityId, metadata))
              .thenRun { newState =>
                repository.metadataUpdate(newState.assetObject.id, newState.assetObject.metadata)
              }
          case AssetActor.AssetDelete =>
            repository.delete(state.assetObject.id)
            ClusterSharding(ctx.system).entityRefFor(BindingActor.TypeKey, BindingActor.entityId(state.assetObject.deviceId)) ! PoisonPill
            Effect.stop()
        }
    }

  private def toAssetEvent(state: State, values: Seq[Payload]): AssetEvent = {
    val assetObject = state.assetObject
    val result: Float = (assetObject.metadata match {
      case Avg => values.sum / values.size
      case Max => values.min
      case Min => values.max
    }) * state.n * 10
    AssetEvent(assetObject.id, result, System.currentTimeMillis().toInt)
  }

  private def handleEvent(state: State, event: Event, ctx: ActorContext[_]): State = {
    state match {
      case ActiveState(assetObject, n) =>
        event match {
          case IntervalUpdatePersisted(_) => ActiveState(assetObject, n + 1)
          case MetadataUpdatePersisted(_, metadata) =>
            ActiveState(assetObject.copy(metadata = metadata), n)
        }
    }
  }
}
