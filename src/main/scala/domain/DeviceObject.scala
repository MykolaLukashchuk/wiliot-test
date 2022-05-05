package domain

import domain.AssetObject.DeviceId
import domain.DeviceObject.{Payload, Timestamp}

case class DeviceObject(timestamp: Timestamp, deviceId: DeviceId, payload: Payload)

object DeviceObject {
  type Payload   = Int
  type Timestamp = Int
}
