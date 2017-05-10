package com.catikkas.aiorserver

import java.net.InetSocketAddress

import akka.actor._
import akka.event.LoggingReceive
import akka.io._
import akka.io.Udp._

class Server(robot: ActorRef) extends Actor with ActorLogging with Config {
  import Server._

  override def preStart(): Unit = {
    self ! Initialize
  }

  def receive: Receive = notInitialized

  def notInitialized: Receive = LoggingReceive.withLabel("notInitialized") {
    case Initialize =>
      import context.system
      IO(Udp) ! Bind(self, new InetSocketAddress(config.port))
      context become initialized
  }

  def initialized: Receive = LoggingReceive.withLabel("initialized") {
    case CommandFailed(_) =>
      log.error("failed to bind")
      context stop self

    case Bound(local) =>
      log.info("server started on {}", local.getPort)
      val socket = sender
      context become bound(socket)
  }

  import Messages._
  import Messages.AiocId._
  import Robot._

  def bound(socket: ActorRef): Receive = LoggingReceive.withLabel("bound") {
    case Received(Aioc(ConnectionReceived), remote) =>
      log.info("connection attempt from {}", remote)
      socket ! Send(UdpConnectionAccepted.toJson, remote)
      context become connected(socket, remote)
  }

  def connected(socket: ActorRef, remote: InetSocketAddress) = LoggingReceive.withLabel(s"connected $remote") {
    case c @ Received(Aioc(ConnectionReceived), _) =>
      // Pretty much drop existing connected client and accept requests only from new client
      context become bound(socket)
      self forward c

    case Received(MouseMove(x, y), `remote`)         => robot ! MouseMoveDelta(x, y)
    case Received(Aioc(MouseLeftPress), `remote`)    => robot ! MousePress(MouseButton.Left)
    case Received(Aioc(MouseLeftRelease), `remote`)  => robot ! MouseRelease(MouseButton.Left)
    case Received(Aioc(MouseRightPress), `remote`)   => robot ! MousePress(MouseButton.Right)
    case Received(Aioc(MouseRightRelease), `remote`) => robot ! MouseRelease(MouseButton.Right)
    case Received(Aioc(MouseWheelDown), `remote`)    => robot ! MouseWheel(WheelDirection.Down)
    case Received(Aioc(MouseWheelUp), `remote`)      => robot ! MouseWheel(WheelDirection.Up)
    case Received(KeyboardString(chars), `remote`)   => robot ! PressKeys(chars)
    case Received(KeyboardInt(int), `remote`)        => robot ! PressKey(int)
    case Received(m, r)                              => log.debug("received unhandled {} from {}", m.utf8String, r)
  }
}

object Server {
  def props(robot: ActorRef) = Props(new Server(robot))

  sealed trait Command
  final case object Initialize extends Command
}
