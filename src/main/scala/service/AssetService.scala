package service

import actors.AssetActor
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import domain.AssetEvent.AssetId
import domain.AssetObject.DeviceId
import domain.{AssetObject, Metadata}
import repo.AssetRepository

trait AssetService {
  def create(assetObject: AssetObject): AssetObject
  def metadataUpdate(id: AssetId, metadata: Metadata)
  def delete(id: AssetId)
  def getAssetIdByDeviceId(deviceId: DeviceId): Option[AssetId]
}

object AssetService {
  def apply()(implicit assetRepository: AssetRepository, system: ActorSystem[_]) =
    new AssetService {
      val sharding: ClusterSharding = ClusterSharding(system)

      def create(assetObject: AssetObject): AssetObject =
        assetRepository.create(assetObject)

      def metadataUpdate(id: AssetId, metadata: Metadata): Unit =
        sharding.entityRefFor(
          AssetActor.TypeKey,
          AssetActor.entityId(id.toString)
        ) ! AssetActor.UpdateMetadata(metadata)

      def delete(id: AssetId): Unit =
        sharding.entityRefFor(
          AssetActor.TypeKey,
          AssetActor.entityId(id.toString)
        ) ! AssetActor.AssetDelete

      def getAssetIdByDeviceId(deviceId: DeviceId): Option[AssetId] =
        assetRepository.getAssetIdByDeviceId(deviceId)
    }
}
