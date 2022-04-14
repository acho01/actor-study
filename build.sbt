name := "actor-study"

version := "0.1"

scalaVersion := "2.13.8"

val AkkaVersion = "2.6.19"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test
)