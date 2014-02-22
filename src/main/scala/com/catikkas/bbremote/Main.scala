package com.catikkas.bbremote

import java.awt.event.InputEvent
import java.net.InetSocketAddress

import akka.actor._
import akka.event.LoggingReceive
import akka.io
import akka.io._
import akka.io.Udp._
import akka.util.ByteString

class Main extends Actor with ActorLogging with Config {
  
  import context.system
  
  var robot = system.deadLetters
  
  override def preStart() {
    io.IO(Udp) ! Bind(self, new InetSocketAddress(port))
    robot = system.actorOf(Robot.props, "robot")
  }
  
  def receive = notBound
  
  def notBound: Receive = LoggingReceive {
    case CommandFailed => {
      log.error("failed to bind")
      context stop self
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
      socket ! Send(UdpConnectionAccepted, remote)
    }
    case Received(MouseMove(x, y), _)         => robot ! MouseMoveDelta(x, y)
    case Received(Aioc(MouseLeftPress), _)    => robot ! MousePress(MouseLeftButton)
    case Received(Aioc(MouseLeftRelease), _)  => robot ! MouseRelease(MouseLeftButton)
    case Received(Aioc(MouseRightPress), _)   => robot ! MousePress(MouseRightButton)
    case Received(Aioc(MouseRightRelease), _) => robot ! MouseRelease(MouseRightButton)
    case Received(Aioc(MouseWheelDown), _)    => robot ! MouseWheel(WheelDirectionDown)
    case Received(Aioc(MouseWheelUp), _)      => robot ! MouseWheel(WheelDirectionUp)
    case Received(KeyboardString(chars), _)   => robot ! PressKeys(chars)
    case Received(m, remote) => log.debug("received unhandled {} from {}", m.utf8String, remote)
  }
}
