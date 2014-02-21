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
    case Received(MoveMouse(x, y), _)         => robot ! MoveMouseDelta(x, y)
    case Received(Aioc(MouseLeftPress), _)    => robot ! MousePress(InputEvent.BUTTON1_DOWN_MASK)
    case Received(Aioc(MouseLeftRelease), _)  => robot ! MouseRelease(InputEvent.BUTTON1_DOWN_MASK)
    case Received(Aioc(MouseRightPress), _)   => robot ! MousePress(InputEvent.BUTTON3_DOWN_MASK)
    case Received(Aioc(MouseRightRelease), _) => robot ! MouseRelease(InputEvent.BUTTON3_DOWN_MASK)
    case Received(m, remote) => log.debug("received unhandled {} from {}", m.utf8String, remote)
  }
  
  val UdpConnectionAccepted: ByteString = {
    val osName = System getProperty "os.name"
    val osVersion = System getProperty "os.version"
    val osArch = System getProperty "os.arch"
    ConnStatus("server", "acceptUdpConnection", "${osName}-${osVersion}-${osArch}")
  }
}