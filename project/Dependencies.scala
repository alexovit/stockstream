import sbt._

object Dependencies {

  // format: OFF
  val scalaLastVersion      = "2.12.5"

  val typesafeConfigVersion = "1.3.3"
  val pureConfigVersion     = "0.9.1"

  val circeVersion          = "0.9.3"
  val akkaVersion           = "2.5.11"

  val scalaTestVersion      = "3.0.5"

  object Compile {
    // Common
    val typesafeConfig       = "com.typesafe"                %  "config"           % typesafeConfigVersion
    val pureConfig           = "com.github.pureconfig"       %% "pureconfig"       % pureConfigVersion

    // Akka
    val akkaActor            = "com.typesafe.akka"   %% "akka-actor"           % akkaVersion
    val akkaStream           = "com.typesafe.akka"   %% "akka-stream"          % akkaVersion

    // JSON
    val circeCore            = "io.circe"            %% "circe-core"          % circeVersion
    val circeGeneric         = "io.circe"            %% "circe-generic"       % circeVersion
  }

  object Test {
    val scalaTest            = "org.scalatest"       %% "scalatest"           % scalaTestVersion  % "test"
  }

  import Compile._
  import Test._

  val common   = Seq(typesafeConfig, pureConfig)
  val io       = Seq(akkaActor, akkaStream, circeCore, circeGeneric)
  val tests    = Seq(scalaTest)

  // format: ON
}