package com.catikkas.bbremote

import akka.actor._
import akka.kernel.Bootable

class Main extends Bootable {
  val system = ActorSystem("bbremote")

  def startup = {
    system.actorOf(Props[Server], "server")
  }

  def shutdown = {
    system.shutdown()
  }
}
