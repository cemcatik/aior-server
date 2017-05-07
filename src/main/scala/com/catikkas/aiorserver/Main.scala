package com.catikkas.aiorserver

object Main extends App {
  import akka.actor._
  val system = ActorSystem("aior-server")
  system.actorOf(Server.props, "server")
}
