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
      val ctrld  = context watch context.actorOf(CtrlD.props, "ctrld")
      context become initialized(robot, server, ctrld)
  }

  def initialized(
      robot: ActorRef,
      server: ActorRef,
      ctrld: ActorRef
  ): Receive = LoggingReceive.withLabel("initialized") {
    case Terminated(`robot`) =>
      log.error("java.awt.Robot terminated. Please make sure environment is not headless.")
      terminate()

    case Terminated(`server`) =>
      log.error("Server failed to bind. Please check config.")
      terminate()

    case Terminated(`ctrld`) => terminate()
  }

  def terminate(): Unit = discarding {
    log.info("Shutting down now.")
    CoordinatedShutdown(context.system).run()
  }

}

object Supervisor {
  def props = Props(new Supervisor)

  sealed trait Command
  final case object Initialize extends Command
}
