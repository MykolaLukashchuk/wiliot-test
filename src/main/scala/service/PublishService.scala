package service

import domain.AssetEvent

trait PublishService {
  def publish(event: AssetEvent)
}

object PublishService {
  def apply(): PublishService = new PublishService {
    override def publish(event: AssetEvent): Unit = ???
  }
}

