package com.catikkas.aiorserver

object Main {

  def main(args: Array[String]): Unit = {
    import akka.actor._
    val system = ActorSystem("aior-server")
    discarding { system.actorOf(Supervisor.props, "sup") }
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
      val robot  = context watch context.actorOf(Robot.props, "robot")
      val server = context watch context.actorOf(Server.props(robot), "server")
      context become initialized(robot, server)
  }

  def initialized(robot: ActorRef, server: ActorRef): Receive = LoggingReceive.withLabel("initialized") {
    case Terminated(`robot`) =>
      log.error("java.awt.Robot terminated. Please make sure environment is not headless.")
      log.error("Shutting down now.")
      discarding { context.system.terminate() }

    case Terminated(`server`) =>
      log.error("Server failed to bind. Please check config.")
      log.error("Shutting down now.")
      discarding { context.system.terminate() }
  }
}

object Supervisor {
  def props = Props(new Supervisor)

  sealed trait Command
  final case object Initialize extends Command
}
