package com.catikkas.aiorserver

object Main {

  def main(args: Array[String]): Unit = {
    import akka.actor._
    val system = ActorSystem("aior-server")
    val _      = system.actorOf(Supervisor.props, "sup")
  }

}

import akka.actor._
import akka.event.LoggingReceive

class Supervisor extends Actor with ActorLogging {
  import Supervisor._

  override def preStart(): Unit = {
    self ! Initialize
  }

  def receive: Receive = notInitialized

  def notInitialized: Receive = LoggingReceive.withLabel("notInitialized") {
    case Initialize =>
      val robot  = context.actorOf(Robot.props, "robot")
      val server = context.actorOf(Server.props(robot), "server")
      val _      = context watch server
      context become initialized(robot, server)
  }

  def initialized(robot: ActorRef, server: ActorRef): Receive = LoggingReceive.withLabel("initialized") {
    case Terminated(`server`) =>
      log.error("Server failed to bind. Please check config.")
      log.error("Shutting down now.")
      val _ = context.system.terminate()
  }
}

object Supervisor {
  def props = Props(new Supervisor)

  sealed trait Command
  final case object Initialize extends Command
}
