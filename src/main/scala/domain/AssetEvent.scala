package domain

import domain.AssetEvent.AssetId
import domain.DeviceObject.Timestamp

case class AssetEvent (assetId: AssetId, value: Float, timestamp: Timestamp)

object AssetEvent {
  type AssetId = Int
}
