package com.catikkas.aiorserver

import akka.actor._
import akka.event.LoggingReceive
import java.awt.{AWTException, MouseInfo, Robot => AwtRobot}
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class Robot extends Actor with ActorLogging with Config {
  import Robot._

  var x = 0
  var y = 0

  var shiftPressed = false

  override def preStart(): Unit = {
    self ! Initialize
  }

  def receive = notInitialized

  def notInitialized = LoggingReceive.withLabel("notInitialized") {
    case Initialize =>
      try {
        val robot = new AwtRobot
        context become initialized(robot)
      } catch {
        case e: AWTException =>
          log.error(e, "Unable to initialize java.awt.Robot")
          context stop self
      }
  }

  def initialized(implicit robot: AwtRobot) = LoggingReceive.withLabel("initialized") {
    case MouseMoveDelta(dx, dy) => mouseMoveDelta(dx, dy)
    case MousePress(button)     => robot.mousePress(button.mask)
    case MouseRelease(button)   => robot.mouseRelease(button.mask)
    case MouseWheel(direction)  => mouseWheel(direction.d)
    case PressKeys(keys)        => pressKeys(keys)
    case PressKey(int)          => pressKey(int)
  }

  def mouseMoveDelta(dx: Int, dy: Int)(implicit robot: AwtRobot): Unit = {
    def newLocation(a: Int, da: Int): Int = {
      val scaled = math.round(da * config.mouseSpeed).intValue
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

  def mouseWheel(direction: Int)(implicit robot: AwtRobot): Unit = {
    val scaled = math.round(direction * config.mouseWheelSpeed).intValue
    robot.mouseWheel(scaled)
  }

  def pressKeys(chars: Seq[Char])(implicit robot: AwtRobot) = chars map Keyboard.keyStroke foreach perform

  def perform(keystroke: KeyStroke)(implicit robot: AwtRobot): Unit = {
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

  def pressKey(int: Int)(implicit robot: AwtRobot): Unit = {
    int2keycode(int) match {
      case KeyEvent.VK_SHIFT => shiftPressed = true
      case x =>
        val modifiers = if (shiftPressed) InputEvent.SHIFT_MASK else 0
        perform(KeyStroke.getKeyStroke(x, modifiers))
        shiftPressed = false
    }
  }

  val int2keycode = Map(
    29 -> KeyEvent.VK_A,
    30 -> KeyEvent.VK_B,
    31 -> KeyEvent.VK_C,
    32 -> KeyEvent.VK_D,
    33 -> KeyEvent.VK_E,
    34 -> KeyEvent.VK_F,
    35 -> KeyEvent.VK_G,
    36 -> KeyEvent.VK_H,
    37 -> KeyEvent.VK_I,
    38 -> KeyEvent.VK_J,
    39 -> KeyEvent.VK_K,
    40 -> KeyEvent.VK_L,
    41 -> KeyEvent.VK_M,
    42 -> KeyEvent.VK_N,
    43 -> KeyEvent.VK_O,
    44 -> KeyEvent.VK_P,
    45 -> KeyEvent.VK_Q,
    46 -> KeyEvent.VK_R,
    47 -> KeyEvent.VK_S,
    48 -> KeyEvent.VK_T,
    49 -> KeyEvent.VK_U,
    50 -> KeyEvent.VK_V,
    51 -> KeyEvent.VK_W,
    52 -> KeyEvent.VK_X,
    53 -> KeyEvent.VK_Y,
    54 -> KeyEvent.VK_Z,
    7  -> KeyEvent.VK_0,
    8  -> KeyEvent.VK_1,
    9  -> KeyEvent.VK_2,
    10 -> KeyEvent.VK_3,
    11 -> KeyEvent.VK_4,
    12 -> KeyEvent.VK_5,
    13 -> KeyEvent.VK_6,
    14 -> KeyEvent.VK_7,
    15 -> KeyEvent.VK_8,
    16 -> KeyEvent.VK_9,
    56 -> KeyEvent.VK_PERIOD,
    70 -> KeyEvent.VK_EQUALS,
    73 -> KeyEvent.VK_BACK_SLASH,
    55 -> KeyEvent.VK_COMMA,
    76 -> KeyEvent.VK_SLASH,
    72 -> KeyEvent.VK_CLOSE_BRACKET,
    71 -> KeyEvent.VK_OPEN_BRACKET,
    62 -> KeyEvent.VK_SPACE,
    66 -> KeyEvent.VK_ENTER,
    59 -> KeyEvent.VK_SHIFT,
    67 -> KeyEvent.VK_BACK_SPACE,
    69 -> KeyEvent.VK_MINUS,
    81 -> KeyEvent.VK_ADD,
    74 -> KeyEvent.VK_SEMICOLON,
    68 -> KeyEvent.VK_BACK_QUOTE
  )
}

object Robot {
  def props = Props(new Robot)

  sealed trait Command
  final case object Initialize                           extends Command
  final case class MouseMoveDelta(dx: Int, dy: Int)      extends Command
  final case class MousePress(button: MouseButton)       extends Command
  final case class MouseRelease(button: MouseButton)     extends Command
  final case class MouseWheel(direction: WheelDirection) extends Command
  final case class PressKeys(chars: Seq[Char])           extends Command
  final case class PressKey(int: Int)

  sealed abstract class MouseButton(val mask: Int)
  object MouseButton {
    object Left  extends MouseButton(InputEvent.BUTTON1_DOWN_MASK)
    object Right extends MouseButton(InputEvent.BUTTON3_DOWN_MASK)
  }

  sealed abstract class WheelDirection(val d: Int)
  object WheelDirection {
    object Up   extends WheelDirection(-1)
    object Down extends WheelDirection(1)
  }
}

object Keyboard {
  // format: off
  val Numbers = ('0' to '9').map { c => c -> KeyStroke.getKeyStroke(c, 0) }.toMap
  val LowerAZ = ('a' to 'z').map { c => c -> KeyStroke.getKeyStroke(c.toUpper, 0) }.toMap
  val UpperAZ = ('A' to 'Z').map { c => c -> KeyStroke.getKeyStroke(c, InputEvent.SHIFT_MASK) }.toMap
  // format: on
  val Whitespaces = Map(
    '\n' -> KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
    '\t' -> KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
    '\b' -> KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
    ' '  -> KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)
  )
  val Layout = io.Source
    .fromURL(Robot.getClass().getResource("/keyboards/US"), "UTF-8")
    .getLines
    .map { l =>
      val c    = l charAt 0
      val spec = l substring 2
      c -> KeyStroke.getKeyStroke(spec)
    }
    .toMap
  val KeyStrokes = Numbers ++ LowerAZ ++ UpperAZ ++ Whitespaces ++ Layout

  def keyStroke(c: Char): KeyStroke = KeyStrokes(c)
}
