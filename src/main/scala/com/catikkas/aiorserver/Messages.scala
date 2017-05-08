package com.catikkas.aiorserver

import akka.util.ByteString
import com.google.gson._
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory

import scala.annotation.tailrec
import scala.reflect._
import scala.util._

object Messages {

  sealed trait Message
  case class Aioc(id: Int) extends Message
  case class ConnStatus(sender: String, status: String, statusMessage: String) extends Message
  case class MouseMove(x: Int, y: Int) extends Message
  case class KeyboardString(letter: String, state: Int = KeyboardString.State) extends Message
  case class KeyboardInt(letter: Int, state: Int = KeyboardInt.State) extends Message

  val UdpConnectionAccepted: Message = {
    val osName = System getProperty "os.name"
    val osVersion = System getProperty "os.version"
    val osArch = System getProperty "os.arch"
    ConnStatus("server", "acceptUdpConnection", s"$osName-$osVersion-$osArch")
  }

  object Aioc {
    val ConnectionReceived = 0
    val MouseLeftPress     = 56
    val MouseLeftRelease   = 57
    val MouseRightPress    = 58
    val MouseRightRelease  = 59
    val MouseWheelDown     = 60
    val MouseWheelUp       = 61

    def unapply(bytes: ByteString): Option[Int] = bytes.parseJson[Aioc] match {
      case Success(Aioc(id)) => Some(id)
      case _ => None
    }
  }

  object MouseMove {
    def unapply(bytes: ByteString): Option[(Int, Int)] = bytes.parseJson[MouseMove] match {
      case Success(MouseMove(x, y)) => Some((x, y))
      case _ => None
    }
  }

  object KeyboardString {
    val State = 3

    def unapply(bytes: ByteString): Option[Seq[Char]] = {
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

      bytes.parseJson[KeyboardString] match {
        case Success(KeyboardString(kbs, KeyboardString.State)) => Some(split(kbs))
        case _ => None
      }
    }
  }

  object KeyboardInt {
    val State = 1

    def unapply(bytes: ByteString): Option[Int] = bytes.parseJson[KeyboardInt] match {
      case Success(KeyboardInt(kbi, KeyboardString.State)) => Some(kbi)
      case _ => None
    }
  }

  implicit val gson: Gson = {
    val maf = RuntimeTypeAdapterFactory.of(classOf[Message], "type")
      .registerSubtype(classOf[Aioc],           "aioc")
      .registerSubtype(classOf[ConnStatus],     "cs")
      .registerSubtype(classOf[MouseMove],      "mmb")
      .registerSubtype(classOf[KeyboardString], "ksb")
      .registerSubtype(classOf[KeyboardInt],    "kib")

    new GsonBuilder()
      .registerTypeAdapterFactory(maf)
      .create()
  }

  type Reads[T] = (ByteString => Try[T])
  type Writes[T] = (T => ByteString)

  implicit def messageReads[T <: Message](implicit t: ClassTag[T], gson: Gson): Reads[T] = { bs =>
    // Fail if m is not an instance of T
    // ClassTag is required
    // Trying to parse to a Message since Gson doesn't fail properly with RuntimeTypeAdapterFactory
    Try {
      gson.fromJson(bs.utf8String, classOf[Message])
    }.flatMap {
      case m: T => Success(m)
      case f => Failure(new MatchError(f))
    }
  }

  implicit def messageWrites(implicit gson: Gson): Writes[Message] = { m =>
    ByteString(gson.toJson(m, classOf[Message]))
  }

  implicit class ByteStringOps(val bs: ByteString) extends AnyVal {
    def parseJson[T](implicit r: Reads[T]): Try[T] = r(bs)
  }

  implicit class MessageOps(val m: Message) extends AnyVal {
    def toJson(implicit w: Writes[Message]): ByteString = w(m)
  }

}
