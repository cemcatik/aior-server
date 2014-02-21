package com.catikkas.bbremote

import akka.actor.Actor

trait Config {
  self: Actor =>

  val config = context.system.settings.config.getConfig("bbremote")
  val port = config.getInt("port")
  val mouseSpeed = config.getDouble("mouseSpeed")
  val mouseWheelSpeed = config.getDouble("mouseWheelSpeed")
}