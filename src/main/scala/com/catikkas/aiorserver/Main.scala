package com.catikkas.aiorserver

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown._
import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter._
import com.catikkas.aiorserver.Main._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

object Main {

  def main(args: Array[String]): Unit = {
    discarding { ActorSystem(Supervisor.behavior, "aior-server") }
  }

  final case class Config(
      port: Int,
      mouseSpeed: Double,
      mouseWheelSpeed: Double
  )

}

object Supervisor {
  sealed trait Command

  def behavior: Behavior[Command] = Behaviors.setup[Command] { implicit context =>
    val config = context.system.settings.config.as[Config]("aiorserver")
    val robot  = context.spawn(Robot.behavior(config), "robot")
    val server = context.actorOf(Server.props(config, robot.toUntyped), "server")
    val ctrld  = context.spawn(CtrlD.behavior, "ctrld")

    Seq(server) foreach { a =>
      context watch a
    }

    Seq(robot, ctrld) foreach { a =>
      context watch a
    }

    Behaviors.logMessages(
      Behaviors.receiveSignal {
        case (_, Terminated(`robot`)) =>
          context.log.error("java.awt.Robot terminated. Please make sure environment is not headless.")
          terminate()
          Behavior.same

        case (_, Terminated(s)) if s == server.toTyped =>
          context.log.error("Server failed to bind. Please check config.")
          terminate()
          Behavior.same

        case (_, Terminated(`ctrld`)) =>
          terminate()
          Behavior.same
      }
    )
  }

  def terminate()(implicit context: ActorContext[Command]): Unit = {
    context.log.info("Shutting down now.")
    discarding { CoordinatedShutdown(context.system.toUntyped).run(JvmExitReason) }
  }

}
