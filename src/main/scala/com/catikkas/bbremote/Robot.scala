package com.catikkas.bbremote

import akka.actor._
import akka.event.LoggingReceive
import java.awt.MouseInfo
import java.awt.{ Robot => AwtRobot }

class Robot extends Actor with ActorLogging with Config {
  import Robot._
  
  val robot = new AwtRobot
  var x = 0
  var y = 0
  
  def receive = LoggingReceive {
    case MoveMouseDelta(x, y) => moveMouseDelta(x, y)
    case MousePress(button)   => robot.mousePress(button)
    case MouseRelease(button) => robot.mouseRelease(button)
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
}

object Robot {
  def props = Props(new Robot)
  
  case class MoveMouseDelta(x: Int, y: Int)
  case class MousePress(mask: Int)
  case class MouseRelease(mask: Int)
}