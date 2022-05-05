name := "wiliot-test"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.8"

val akkaVersion = "2.6.18"
val scalaTestVersion = "3.2.9"
val logbackVersion = "1.2.10"
val kafkaVersion = "3.1.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "org.apache.kafka" %% "kafka" % kafkaVersion,
  "org.apache.kafka" %% "kafka-streams-scala" % kafkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0",

  "org.scalatest" %% "scalatest" % scalaTestVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
)
