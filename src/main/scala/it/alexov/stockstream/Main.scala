package it.alexov.stockstream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import pureconfig.loadConfigOrThrow
import scala.io.StdIn

object Main extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher
  implicit val logger = system.log

  implicit val serverConfig = loadConfigOrThrow[Config]

  try {
    val server = new Server()
    server.run()
    println(">>> Press ENTER to exit <<<")
    StdIn.readLine()
  } finally {
    system.terminate()
  }

}
