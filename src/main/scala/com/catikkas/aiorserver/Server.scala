package com.catikkas.aiorserver

import java.net.InetSocketAddress

import akka.actor._
import akka.event.LoggingReceive
import akka.io._
import akka.io.Udp._

class Server extends Actor with ActorLogging with Config {
  import context.system

  var robot = system.deadLetters

  override def preStart(): Unit = {
    IO(Udp) ! Bind(self, new InetSocketAddress(config.port))
    robot = context.actorOf(Robot.props, "robot")
  }

  def receive = notBound

  def notBound: Receive = LoggingReceive {
    case CommandFailed(_) => {
      log.error("failed to bind")
      context.system.terminate()
    }
    case Bound(local) => {
      log.info("server started on {}", local.getPort)
      val socket = sender
      context become bound(socket)
    }
  }

  import Messages._
  import Messages.Aioc._
  import Robot._

  def bound(socket: ActorRef): Receive = LoggingReceive {
    case Received(Aioc(ConnectionReceived), remote) => {
      log.info("connection attempt from {}", remote)
      socket ! Send(UdpConnectionAccepted.toJson, new InetSocketAddress(remote.getAddress, config.port))
    }
    case Received(MouseMove(x, y),         _) => robot ! MouseMoveDelta(x, y)
    case Received(Aioc(MouseLeftPress),    _) => robot ! MousePress(MouseLeftButton)
    case Received(Aioc(MouseLeftRelease),  _) => robot ! MouseRelease(MouseLeftButton)
    case Received(Aioc(MouseRightPress),   _) => robot ! MousePress(MouseRightButton)
    case Received(Aioc(MouseRightRelease), _) => robot ! MouseRelease(MouseRightButton)
    case Received(Aioc(MouseWheelDown),    _) => robot ! MouseWheel(WheelDirectionDown)
    case Received(Aioc(MouseWheelUp),      _) => robot ! MouseWheel(WheelDirectionUp)
    case Received(KeyboardString(chars),   _) => robot ! PressKeys(chars)
    case Received(KeyboardInt(int),        _) => robot ! PressKey(int)
    case Received(m, remote) => log.debug("received unhandled {} from {}", m.utf8String, remote)
  }
}

object Server {
  def props = Props(new Server)
}
