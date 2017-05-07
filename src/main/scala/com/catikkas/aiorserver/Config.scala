package com.catikkas.aiorserver

import akka.actor.Actor

trait Config { self: Actor =>

  object config {
    val config = context.system.settings.config.getConfig("aiorserver")
    val port = config.getInt("port")
    val mouseSpeed = config.getDouble("mouseSpeed")
    val mouseWheelSpeed = config.getDouble("mouseWheelSpeed")
  }

}
