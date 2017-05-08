package com.catikkas.aiorserver

object Main {

  def main(args: Array[String]): Unit = {
    import akka.actor._
    val system = ActorSystem("aior-server")
    val _ = system.actorOf(Server.props, "server")
  }

}
