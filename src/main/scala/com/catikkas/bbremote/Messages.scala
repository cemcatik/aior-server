package com.catikkas.bbremote

import com.google.gson.Gson
import akka.util.ByteString
import scala.util.Try
import scala.util.Success
import scala.annotation.tailrec

object Messages {

  trait Message {
    val `type`: String
  }
  case class Aioc(id: Int, `type`: String = "aioc") extends Message
  case class ConnStatus(sender: String, status: String, statusMessage: String, `type`: String = "cs") extends Message
  case class MouseMove(x: Int, y: Int, `type`: String = "mmb") extends Message
  case class KeyboardString(letter: String, state: Int = 3, `type`: String = "ksb") extends Message
  case class KeyboardInt(letter: Int, state: Int = 1, `type`: String = "kib")
  
  val gson = new Gson
  
  val UdpConnectionAccepted: ByteString = {
    val osName = System getProperty "os.name"
    val osVersion = System getProperty "os.version"
    val osArch = System getProperty "os.arch"
    ConnStatus("server", "acceptUdpConnection", "${osName}-${osVersion}-${osArch}")
  }

  object Aioc {
    val ConnectionReceived = 0
    val MouseLeftPress     = 56
    val MouseLeftRelease   = 57
    val MouseRightPress    = 58
    val MouseRightRelease  = 59
    val MouseWheelDown     = 60
    val MouseWheelUp       = 61
    
    def unapply(bytes: ByteString): Option[Int] = Try(gson.fromJson(bytes.utf8String, classOf[Aioc])) match {
      case Success(Aioc(id, "aioc")) => Some(id)
      case _ => None
    }
  }
  
  object MouseMove {
    def unapply(bytes: ByteString): Option[(Int, Int)] = Try(gson.fromJson(bytes.utf8String, classOf[MouseMove])) match {
      case Success(MouseMove(x, y, "mmb")) => Some((x, y))
      case _ => None
    }
  }
  
  object KeyboardString {
    def unapply(bytes: ByteString): Option[Seq[Char]] = Try(gson.fromJson(bytes.utf8String, classOf[KeyboardString])) match {
      case Success(KeyboardString(kbs, 3, "ksb")) => Some(split(kbs))
      case _ => None
    }

    def split(kbs: String): Seq[Char] = {
      @tailrec
      def split(s: String, acc: Seq[String]): Seq[String] = {
        s indexOf "--" match {
          case -1 => acc :+ s
          case 0  => split(s.substring(3), acc :+ "-")
          case x  => {
            val l = s.substring(0, x)
            val rest = s.substring(x + 2)
            split(rest, acc :+ l)
          }
        }
      }

      def charOrSpecial(l: String): Char = l match {
        case "backspace" => '\b'
        case "enter"     => '\n'
        case "space"     => ' '
        case x           => x.head
      }

      split(kbs, Seq()) map charOrSpecial
    }
  }

  object KeyboardInt {
    def unapply(bytes: ByteString): Option[Int] = Try(gson.fromJson(bytes.utf8String, classOf[KeyboardInt])) match {
      case Success(KeyboardInt(kib, 1, "kib")) => Some(kib)
      case _ => None
    }
  }

  implicit def message2Json(m: Message): ByteString = ByteString(gson.toJson(m))
}
