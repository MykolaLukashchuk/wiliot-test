package domain

import domain.AssetObject.DeviceId

case class AssetObject(id: Int, metadata: Metadata = Min, deviceId: DeviceId) {

}

object AssetObject {
  type DeviceId = Int
}

sealed trait Metadata

case object Avg extends Metadata
case object Min extends Metadata
case object Max extends Metadata

object Metadata {
}

sealed trait Reply
case object Success extends Reply
case object Failure extends Reply