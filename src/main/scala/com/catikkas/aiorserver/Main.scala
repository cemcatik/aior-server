package com.catikkas.aiorserver

import java.util.Scanner

import akka.actor._
import akka.event.LoggingReceive

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global


object Main {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("aior-server")
    discarding { system.actorOf(Supervisor.props, "sup") }

    system.log.info("Use Ctrl+D to stop.")
    discarding { new Scanner(System.in).asScala.find(_ == null) }
    Await.result(terminate(), Duration.Inf)
  }

  def terminate()(implicit ec: ExecutionContext, system: ActorSystem): Future[Unit] = {
    system.log.info("Shutting down now.")

    CoordinatedShutdown(system).run().map(_ => ())(ec)
  }

}

class Supervisor extends Actor with ActorLogging {
  import context.system
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
      discarding { Main.terminate() }

    case Terminated(`server`) =>
      log.error("Server failed to bind. Please check config.")
      discarding { Main.terminate() }
  }
}

object Supervisor {
  def props = Props(new Supervisor)

  sealed trait Command
  final case object Initialize extends Command
}
