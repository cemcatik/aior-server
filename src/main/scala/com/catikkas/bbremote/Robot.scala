package com.catikkas.bbremote

import akka.actor._
import akka.event.LoggingReceive
import java.awt.MouseInfo
import java.awt.{ Robot => AwtRobot }
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class Robot extends Actor with ActorLogging with Config {
  import Robot._
  
  val robot = new AwtRobot
  var x = 0
  var y = 0
  
  def receive = LoggingReceive {
    case MouseMoveDelta(dx, dy) => mouseMoveDelta(dx, dy)
    case MousePress(button)     => robot.mousePress(button)
    case MouseRelease(button)   => robot.mouseRelease(button)
    case MouseWheel(direction)  => mouseWheel(direction)
    case PressKeys(keys)        => pressKeys(keys)
  }
  
  def mouseMoveDelta(dx: Int, dy: Int) {
    def newLocation(a: Int, da: Int): Int = {
      val scaled = math.round(da * mouseSpeed).intValue
      a + scaled
    }

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
  
  def mouseWheel(direction: Int) {
    val scaled = math.round(direction * mouseWheelSpeed).intValue
    robot.mouseWheel(scaled)
  }

  def pressKeys(chars: Seq[Char]) {
    import Keyboard._

    def perform(keystroke: KeyStroke) {
      val keycode = keystroke.getKeyCode
      val isShift = (keystroke.getModifiers & InputEvent.SHIFT_MASK) != 0

      if (isShift) {
        robot keyPress KeyEvent.VK_SHIFT
      }

      robot keyPress keycode
      robot keyRelease keycode

      if (isShift) {
        robot keyRelease KeyEvent.VK_SHIFT
      }
    }

    chars map keyStroke foreach perform
  }
}

object Robot {
  def props = Props(new Robot)
  
  case class MouseMoveDelta(dx: Int, dy: Int)
  case class MousePress(mask: Int)
  case class MouseRelease(mask: Int)
  case class MouseWheel(direction: Int)
  case class PressKeys(chars: Seq[Char])

  val MouseLeftButton = InputEvent.BUTTON1_DOWN_MASK
  val MouseRightButton = InputEvent.BUTTON3_DOWN_MASK
  val WheelDirectionUp = -1
  val WheelDirectionDown = 1
}

object Keyboard {
  val Numbers = ('0' to '9') map { c => c -> KeyStroke.getKeyStroke(c, 0) } toMap
  val LowerAZ = ('a' to 'z') map { c => c -> KeyStroke.getKeyStroke(c.toUpper, 0) } toMap
  val UpperAZ = ('A' to 'Z') map { c => c -> KeyStroke.getKeyStroke(c, InputEvent.SHIFT_MASK) } toMap
  val Whitespaces = Map(
    '\n' -> KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
    '\t' -> KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
    '\b' -> KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
    ' '  -> KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)
  )
  val Layout = io.Source.fromURL(Robot.getClass().getResource("/keyboards/US"), "UTF-8").getLines map { l =>
    val c = l charAt 0
    val spec = l substring 2
    c -> KeyStroke.getKeyStroke(spec)
  } toMap
  val KeyStrokes = Numbers ++ LowerAZ ++ UpperAZ ++ Whitespaces ++ Layout

  def keyStroke(c: Char): KeyStroke = KeyStrokes(c)
}
