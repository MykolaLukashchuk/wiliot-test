import actors.BindingActor
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import domain.DeviceObject
import domain.DeviceObject.{Payload, Timestamp}
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KeyValue, StreamsBuilder}
import org.apache.kafka.streams.kstream.{Consumed, TimeWindows}

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

object KafkaStreamsTopology {
  private implicit val timeout: Timeout = Timeout(11, TimeUnit.SECONDS)

  def build(implicit system: ActorSystem[_]): StreamsBuilder = {
    val builder: StreamsBuilder = new StreamsBuilder
    val sharding: ClusterSharding = ClusterSharding(system)

    builder.stream[String, DeviceObject](
      "device-data-queue",
      Consumed.`with`(Serdes.String(), Serdes.serdeFrom(classOf[DeviceObject])))
      .map((_, v) => KeyValue.pair(v.deviceId, v))
      .groupByKey()
      .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.of(5, java.time.temporal.ChronoUnit.MINUTES)))
      .aggregate[Seq[(Timestamp, Payload)]](
        () => Seq(),
        (_, newValue, aggValue) => aggValue :+ (newValue.timestamp -> newValue.payload)
      ).toStream
      .foreach((key, v) => {
        val deviceId = key.key().toInt
        val target = sharding.entityRefFor(BindingActor.TypeKey, BindingActor.entityId(deviceId))
        val f = target ? (replyTo => BindingActor.DeviceMsgs(deviceId, v, replyTo))
        Try(Await.result(f, timeout.duration)) match {
          case Success(value) => value match {
            case domain.Success => value
            case domain.Failure => throw new RuntimeException("Something went wrong")
          }
          case Failure(exception) => throw exception
        }
      })
    builder
  }
}
