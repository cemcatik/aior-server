package com.catikkas.aiorserver

import java.net.InetSocketAddress

import akka.actor._
import akka.event.LoggingReceive
import akka.io._
import akka.io.Udp._

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class Server extends Actor with ActorLogging with Config {
  var robot = context.system.deadLetters

  override def preStart(): Unit = {
    import context.system
    IO(Udp) ! Bind(self, new InetSocketAddress(config.port))
    robot = context.actorOf(Robot.props, "robot")
  }

  def receive = notBound

  def notBound = LoggingReceive.withLabel("notBound") {
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

  def bound(socket: ActorRef) = LoggingReceive.withLabel("bound") {
    case Received(Aioc(ConnectionReceived), remote) =>
      log.info("connection attempt from {}", remote)
      socket ! Send(UdpConnectionAccepted.toJson, new InetSocketAddress(remote.getAddress, config.port))
      context become connected(remote)
  }

  def connected(remote: InetSocketAddress) = LoggingReceive.withLabel(s"connected $remote") {
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
  def props = Props(new Server)
}
