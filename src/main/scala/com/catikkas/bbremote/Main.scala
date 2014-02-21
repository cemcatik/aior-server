package com.catikkas.bbremote

import java.awt.MouseInfo
import java.awt.Robot
import java.net.InetSocketAddress
import akka.actor._
import akka.event.LoggingReceive
import akka.io
import akka.io._
import akka.io.Udp._
import akka.util.ByteString
import java.awt.event.InputEvent
import javax.swing.SwingUtilities

class Main extends Actor with ActorLogging with Config {
  
  import context.system
  
  val robot = new Robot
  var x: Int = _
  var y: Int = _
  
  override def preStart() {
    io.IO(Udp) ! Bind(self, new InetSocketAddress(port))
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
  
  def bound(socket: ActorRef): Receive = LoggingReceive {
    case Received(Aioc(ConnectionReceived), remote) => {
      log.info("connection attempt from {}", remote)
      socket ! Send(UdpConnectionAccepted, remote)
    }
    case Received(MoveMouse(x, y), _)         => moveMouseDelta(x, y)
    case Received(Aioc(MouseLeftPress), _)    => mouseLeftPress
    case Received(Aioc(MouseLeftRelease), _)  => mouseLeftRelease
    case Received(Aioc(MouseRightPress), _)   => mouseRightPress
    case Received(Aioc(MouseRightRelease), _) => mouseRightRelease
    case Received(m, remote) => log.debug("received unhandled {} from {}", m.utf8String, remote)
  }
  
  val UdpConnectionAccepted: ByteString = {
    val osName = System getProperty "os.name"
    val osVersion = System getProperty "os.version"
    val osArch = System getProperty "os.arch"
    ConnStatus("server", "acceptUdpConnection", "${osName}-${osVersion}-${osArch}")
  }
  
  def moveMouseDelta(dx: Int, dy: Int) {
    val pointerInfo = MouseInfo.getPointerInfo
    if (pointerInfo == null) {
      robot.mouseMove(x, y)
    } else {
      val point = pointerInfo.getLocation
      x = point.x
      y = point.y
      robot.mouseMove(newLocation(x, dx), newLocation(y, dy))
    }
  }
  
  def newLocation(a: Int, da: Int): Int = {
    val scaled = math.round(da * mouseSpeed).intValue
    a + scaled
  }
  
  def mouseLeftPress {
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
  }
  
  def mouseLeftRelease {
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
  }
  
  def mouseRightPress {
    robot.mousePress(InputEvent.BUTTON3_DOWN_MASK)
  }
  
  def mouseRightRelease {
    robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK)
  }
}