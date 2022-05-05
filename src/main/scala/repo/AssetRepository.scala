package repo

import domain.AssetEvent.AssetId
import domain.{AssetObject, Metadata}
import domain.AssetObject.DeviceId

trait AssetRepository {
  def find(id: Int): Option[AssetObject]
  def create(assetObject: AssetObject): AssetObject
  def getAssetIdByDeviceId(deviceId: DeviceId): Option[AssetId]
  def metadataUpdate(assetId: AssetId, metadata: Metadata)
  def delete(id: AssetId)
}

object AssetRepository {

  def apply(): AssetRepository = new AssetRepository {

    def find(id: Int): Option[AssetObject] = ???
    def create(assetObject: AssetObject): AssetObject = ???
    def getAssetIdByDeviceId(deviceId: DeviceId): Option[AssetId] = ???
    def metadataUpdate(assetId: AssetId, metadata: Metadata): Unit = ???

    def delete(id: AssetId): Unit = ???
  }
}
