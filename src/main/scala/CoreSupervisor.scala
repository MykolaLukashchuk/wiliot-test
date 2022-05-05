import actors.{AssetActor, BindingActor}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import org.apache.kafka.streams.KafkaStreams
import repo.{AssetRepository, AssetService}
import service.PublishService

import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Root actor bootstrapping the application
 */
object CoreSupervisor {

  private implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)

  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>

    implicit val system = ctx.system
    implicit val repository: AssetRepository = AssetRepository()
    implicit val kafkaProducer: PublishService = PublishService()
    implicit val assetService: AssetService = AssetService()
    AssetActor.initShard
    BindingActor.initShard

    def properties: Properties = ???

    val streams = new KafkaStreams(KafkaStreamsTopology.build.build(), properties)
    streams.start()

    Behaviors.same
  }
}