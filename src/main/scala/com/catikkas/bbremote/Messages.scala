package com.catikkas.bbremote

import com.google.gson.Gson
import akka.util.ByteString
import scala.util.Try
import scala.util.Success

object Messages {

  trait Message {
    val `type`: String
  }
  case class Aioc(id: Int, `type`: String = "aioc") extends Message
  case class MoveMouse(x: Int, y: Int, `type`: String = "mmb") extends Message
  case class ConnStatus(sender: String, status: String, statusMessage: String, `type`: String = "cs") extends Message
  
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
    
    def unapply(bytes: ByteString): Option[Int] = Try(gson.fromJson(bytes.utf8String, classOf[Aioc])) match {
      case Success(Aioc(id, "aioc")) => Some(id)
      case _ => None
    }
  }
  
  object MoveMouse {
    def unapply(bytes: ByteString): Option[(Int, Int)] = Try(gson.fromJson(bytes.utf8String, classOf[MoveMouse])) match {
      case Success(MoveMouse(x, y, "mmb")) => Some((x, y))
      case _ => None
    }
  }
  
  implicit def message2Json(m: Message): ByteString = ByteString(gson.toJson(m))
}