package com.catikkas.aiorserver

import akka.actor._
import akka.kernel.Bootable

class Main extends Bootable {
  val system = ActorSystem("aior-server")

  def startup = {
    system.actorOf(Server.props, "server")
  }

  def shutdown = {
    system.shutdown()
  }
}
