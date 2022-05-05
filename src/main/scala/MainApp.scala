import akka.NotUsed
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory

object MainApp {
  def main(args: Array[String]): Unit = {
    val system: ActorSystem[NotUsed] = ActorSystem[NotUsed](CoreSupervisor(), "ClusterSystem", ConfigFactory.load("clusterConfig.conf"))
  }
}